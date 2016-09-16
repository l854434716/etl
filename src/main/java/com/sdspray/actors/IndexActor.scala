package com.sdspray.actors

import akka.actor.{ActorSystem, ActorRef, ActorLogging, Actor}
import com.google.gson.Gson
import com.sdspray.core.Configuration
import play.twirl.api.Html
import spray.http.StatusCodes
import spray.http.MediaTypes._
import com.sdspray.domain.Person
import spray.httpx.Json4sSupport
import spray.httpx.encoding.Gzip
import spray.routing.Directives

/**
  * Created by JiangFeng on 2014/4/25.
  */


class IndexService(implicit system: ActorSystem) extends Directives {

  lazy val route =
    pathPrefix("page") {
      val dir = "page/"
      pathEndOrSingleSlash {
        getFromResource(dir + "index.html")
      } ~
        getFromResourceDirectory(dir)
    } ~ pathSingleSlash {
      complete("亲爱的梦瑶我爱你,我是你家的猪,今天是我睡懒觉害你又没玩成.对不起")
    } ~
      path("echo" / Segment) {
        message => get {
          complete {
            s"${message}"
          }
        }
      } ~
      path("person") {
        get {
          complete {
            val person = new Person("Feng Jiang", 26)
            new Gson().toJson(person)
          }
        }
      } ~ path("html" / Segment) {
      message => get {
        respondWithMediaType(`text/html`) {
          encodeResponse(Gzip) {
            complete(/*page.html.index(message).toString()*/ "")
          }
        }
      }
    }
}
