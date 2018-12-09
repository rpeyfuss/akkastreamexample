package com.reginapeyfuss.services

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests the akka stream example
 */
@Singleton
class AkkaExampleController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def streamData() = Action { implicit request: Request[AnyContent] =>
    Ok("stream")
  }
}
