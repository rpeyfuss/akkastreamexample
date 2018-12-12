package com.reginapeyfuss.services.httpClient

import java.util.concurrent.TimeoutException

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ConnectionException}
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.reginapeyfuss.services.models.MetaWeather
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._


@Singleton
class HttpClient @Inject()(ws: WSClient, implicit val actorSystem: ActorSystem) {

	val log = Logger(this.getClass)

	def getCityMetaData(city: String): Future[Either[String, List[MetaWeather]]] = {
		val connectionUrl = s"https://wwww.metaweather.com/api/location/search/?query=$city"
		val req = ws.url(connectionUrl)
				.withRequestTimeout(10 seconds)
				.get()

		val result = req.map {
			response => {
				response.status match {
					case 200 =>
						val data = response.json.validate[List[MetaWeather]]
						data match {
							case s: JsSuccess[List[MetaWeather]] =>
								log.warn(s"The return data is ${s.get}")
								Right(s.get)
							case e: JsError =>
								log.error(s"${response.status} - Json parsing error in getting campaigns for url:<< ${connectionUrl}" +
										s"with error ${response.json.toString}")
								Left("Json Error")
						}

					case code: Int =>
						log.error(code + s" error in getting Planner campaigns found processing url:>> " +
								s"${connectionUrl} ")
						Left(s"$code Error")
				}
			}
		}
		result.onComplete {
			case Success(_) => // do nothings
			case Failure(e) => log.warn(s"error in getting getCityMetaData http request", e)
		}

		result.recover{
			case te: TimeoutException => Left("Timeout Error")
			case ce: ConnectionException => Left("Connection Error")
		}
		result
	}


}

