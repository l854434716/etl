package com.sdspray.http.service

import akka.actor.ActorSystem
import com.google.gson.Gson
import com.lambdaworks.redis.RedisClient
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import spray.http.MediaTypes._
import spray.httpx.encoding.Gzip
import spray.routing.Directives

/**
  * Created by LENOVO on 2016/9/19.
  */
class RedisService(implicit actorRefFactory: ActorSystem) extends Directives {

  val  logger= LoggerFactory.getLogger(this.getClass.getName)
  val redisclient = RedisClient.create("redis://192.168.2.225:6379")
  val connection = redisclient.connect()
  val monitorRoutes = {
    get {
      path("redis" / "key" / Segment) {
        key => get {

          respondWithMediaType(`application/json`) {
            encodeResponse(Gzip) {
              complete {

                val result = connection.`type`(key) match {
                  case "hash" => connection.hgetall(key)
                  case "none" => "key不存在"
                  case "string" => connection.get(key)
                  case "list" => {}
                  case "set" =>
                  case "zset" =>
                }
                logger.info(s"qurey key ${key} result = ${result.toString}")
                new Gson().toJson(result)
              }
            }

          }

        }

      }
    } ~
      (post | parameter('method ! "post")) {
        path("stop") {
          complete {

            ""
          }
        }
      }
  }

}
