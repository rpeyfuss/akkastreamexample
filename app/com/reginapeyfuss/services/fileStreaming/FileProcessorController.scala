package com.reginapeyfuss.services.fileStreaming

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import javax.inject._
import play.api.Logger
import play.api.mvc._

import scala.concurrent.TimeoutException


/**
 * This controller creates an `Action` to handle HTTP requests the akka stream example
 */
@Singleton
class FileProcessorController @Inject()(cc: ControllerComponents,
                                      fileProcessingManager: FileProcessorManager,
                                      system: ActorSystem)
        extends AbstractController(cc) {

  implicit val actorSystem: ActorSystem = system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val log = Logger(this.getClass)

  def streamData(): WebSocket = WebSocket.accept[String, String] { request =>

	  // Ignore incomming messages
	  val in = Sink.ignore

	  // Send  message and then leave the socket open
	  val out =  fileProcessingManager.dataProcessding()
	  		  .recover {
				  case te: TimeoutException => "message: Timeout exception"
			  }
	  Flow.fromSinkAndSource(in, out)
  }




	//	  Action { implicit request: Request[AnyContent] =>
//      val fileSource: Source[String, NotUsed] = fileProcessingManager.liveProcessingFiles()
//		  .recover {
//			  case timeoutExc: TimeoutException => "{\"message\": \"" + timeoutExc + "\"}"
//		  }
//      Ok.chunked(fileSource).as("text/plain")
//     }
}
