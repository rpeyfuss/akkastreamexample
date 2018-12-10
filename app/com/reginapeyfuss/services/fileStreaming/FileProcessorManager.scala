package com.reginapeyfuss.services.fileStreaming

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource
import akka.stream.scaladsl.{Compression, FileIO, Flow, Framing, Source}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


case class FileReadings(id: String, value: Double)

object FileReadings{
	implicit val fileReadingsWrite: Writes[FileReadings] = Json.writes[FileReadings]
	implicit val fileReadingsReads: Reads[FileReadings] = Json.reads[FileReadings]
}

@Singleton
class FileProcessorManager @Inject()(config: Configuration, actorSystem: ActorSystem){
	implicit val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
	private val log = Logger(this.getClass)

	private val defaultAvgThreshold = 0.0
	private val defaultAvgThresholdConfig = "file-proocessor.avg-threshold"
	private val averageThreshold = config.getOptional[Double](defaultAvgThresholdConfig).getOrElse(defaultAvgThreshold)
	private val defaultDir = "resources"
	private val defaultDirConfig = "file-processor.data-dir"
	private val dataDir = config.getOptional[String](defaultDirConfig).getOrElse(defaultDir)

	private val defaultInterval = "3.seconds"
	private val defaultIntervalConfig = "file-processor.interval"
	private val polIntervalStr = config.getOptional[String](defaultIntervalConfig).getOrElse(defaultInterval)
	private val polInterval = FiniteDuration.apply(polIntervalStr.substring(0, polIntervalStr.indexOf(".")).toLong,
		polIntervalStr.substring(polIntervalStr.indexOf(".")+1))

	private val newFiles: Source[(Path, DirectoryChange), NotUsed] = DirectoryChangesSource.apply(Paths.get(dataDir), polInterval, 128)

	def liveProcessingFiles(): Source[String, NotUsed] = {
		log.warn(s"starting file streaming")
			newFiles
			.via(gzJsonPaths)
			.via(fileBytes)
			.via(decompressGZip)
			.via (splitByNewLine)
			.via(parseToJson)
			.via(parseToObj[FileReadings])
			.via(average)
				.log("average is")
				.withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel, onFinish = Logging.WarningLevel, onFailure = Logging.ErrorLevel))
			.via(average2ByteString)
	}

	val gzJsonPaths: Flow[(Path, DirectoryChange), Path, NotUsed] =
		Flow[(Path, DirectoryChange)]
			.filter(pair => pair._1.toString.endsWith(".json.gz") && pair._2.equals(DirectoryChange.Creation))
        	.map(p => {
				log.warn (s"file path is: $p._1")
				p._1
			})

	val fileBytes: Flow[Path, ByteString, NotUsed] = Flow[Path].flatMapConcat(path => FileIO.fromPath(path))

	val decompressGZip: Flow[ByteString, ByteString, NotUsed] = {
		Flow[ByteString].via(Compression.gunzip())
	}

	val splitByNewLine: Flow[ByteString, String, NotUsed] = {
		Flow[ByteString]
				.via(Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true))
				.map(_.utf8String)
	}

	val parseToJson: Flow[String, Option[JsValue], NotUsed] = {
		Flow[String].map(
			sourceStr =>
				Try(Json.parse(sourceStr)) match {
					case Success(result) => Some(result)
					case Failure(e) => {
						log.error(s" in flowParseToJson()n - Error parsing Json value from source string", e)
						None
					}
				}
		)
	}

	def parseToObj[T: Reads]:  Flow[Option[JsValue], Option[T], NotUsed] = {
		Flow[Option[JsValue]].map {
			case Some(jsVal) =>
				val result = jsVal.validate[T]
				result match {
					case xs: JsSuccess[T] => Some(result.get)
					case e: JsError =>
						log.error(s"in parseToObj() - failed to parse jsvalue:  $jsVal")
						None
				}
			case None => None
		}
	}

	val average: Flow[Option[FileReadings], Double, NotUsed] = {
		Flow[Option[FileReadings]]
			.grouped(2)
			.mapAsyncUnordered(10)(pair => Future.successful{
				pair.map {
					case Some(reading) => reading.value
					case None => -1
				}
					.sum/2
			})
			.filter(p => p > averageThreshold)

	}
	val average2ByteString: Flow[Double, String, NotUsed] = Flow[Double].map(p =>  p.toString)

}
