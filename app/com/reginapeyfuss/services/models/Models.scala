package com.reginapeyfuss.services.models

import play.api.libs.json.{Json, Reads, Writes}

case class MetaWeather(title: Option[String], location_type: Option[String],
					   woeid: Option[Long], latt_long:Option[String])

object MetaWeather{
	implicit val fileReadingsWrite: Writes[MetaWeather] = Json.writes[MetaWeather]
	implicit val fileReadingsReads: Reads[MetaWeather] = Json.reads[MetaWeather]
}


case class City(title: Option[String], timezone: Option[String], timezone_name: Option[String],
							   woeid: Option[Long], time: Option[String], error: Option[String] = None)

object City{
	implicit val fileReadingsWrite: Writes[City] = Json.writes[City]
	implicit val fileReadingsReads: Reads[City] = Json.reads[City]
}

case class Message (name: String, msg: String)
object Messge{
	implicit val fileReadingsWrite: Writes[Message] = Json.writes[Message]
	implicit val fileReadingsReads: Reads[Message] = Json.reads[Message]
}