package com.sdspray.core

import java.io.File

import akka.actor.ActorSystem
import akka.io.IO
import cluster.startup.Content
import com.sdspray.core.Server._
import com.sdspray.http.Routes
import com.typesafe.config.ConfigFactory
import spray.can.Http

/**
  * Created by LENOVO on 2016/9/14.
  */
class SprayService(val system: ActorSystem) extends Routes {

  import SprayService._

  val host = config.getString("http.server.host")
  val portHttp = config.getInt("http.server.ports.http")

  def startHttpService(): Unit = {

    IO(Http) ! Http.Bind(httpServer, host, port = portHttp)
  }

}

object SprayService {

  val config = ConfigFactory.parseFile(new File(Content.DEFAULT_SPRAY_CONF_PATH))

  def apply(system: ActorSystem): SprayService = {
    new SprayService(system)
  }
}
