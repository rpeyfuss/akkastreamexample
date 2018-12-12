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
import com.reginapeyfuss.services.httpClient.HttpClient
import com.reginapeyfuss.services.models.{City, Message, MetaWeather}
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class FileProcessorManager @Inject()(config: Configuration, httpClient: HttpClient,
									 actorSystem: ActorSystem){
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

	val newFiles: Source[(Path, DirectoryChange), NotUsed] = DirectoryChangesSource.apply(Paths.get(dataDir), polInterval, 128)

	def dataProcessding(): Source[String, NotUsed] = {
		log.warn(s"starting file streaming")
			newFiles
			.via(filePaths)
			.via(fileBytes)
			.via (splitByNewLine)
				.log( "split by new line")
				.withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel, onFinish = Logging.WarningLevel, onFailure = Logging.ErrorLevel))

			.via(wordCount)
					.log( "count words")
					.withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel, onFinish = Logging.WarningLevel, onFailure = Logging.ErrorLevel))

	}

	val filePaths: Flow[(Path, DirectoryChange), Path, NotUsed] =
		Flow[(Path, DirectoryChange)]
			.filter(pair => pair._1.toString.endsWith(".csv") && pair._2.equals(DirectoryChange.Creation))
        	.map(p => p._1)

	val fileBytes: Flow[Path, ByteString, NotUsed] = Flow[Path].flatMapConcat(path => FileIO.fromPath(path))

	val splitByNewLine: Flow[ByteString, String, NotUsed] = {
		Flow[ByteString]
				.via(Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true))
				.map(_.utf8String)
	}

	val sleep: Flow[String, String, NotUsed] = Flow[String].map {
		str => {
			Thread.sleep(1000)
			str
		}
	}
	val wordCount: Flow[String, String, NotUsed] = Flow[String].mapAsync(5) (
		str => {
			val list = str.split(",")
			val count = list.apply(1).split(" ").length
			Future.successful(s"${list.apply(0)}: word count is $count ")
		})



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

	val cityData: Flow[Either[City, Option[MetaWeather]], String, NotUsed] = Flow[Either[City, Option[MetaWeather]]].map {
		metaWeather => {
			metaWeather match {
				case Right(meta) => if (meta.isDefined) meta.get.title.getOrElse("no city specified")
				else ("Error")
				case Left(err) => err.error.getOrElse("no error available")
			}
		}
	}

	val metaWeatherData: Flow[String, Either[City, Option[MetaWeather]], NotUsed] =
		Flow[String].mapAsync(2)(city => {
			val result: Future[Either[String, List[MetaWeather]]] = httpClient.getCityMetaData(city)
			result.map {
				res => res match {
					case Right(metaData) => Right(metaData.headOption)
					case Left(err) => Left(City(None, None, None, None, Some(err)))
				}
			}
		})

}
