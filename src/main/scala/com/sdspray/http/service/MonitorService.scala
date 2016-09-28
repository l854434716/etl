package com.sdspray.http.service

import java.util

import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.Member
import cluster.actor.master.MasterActor
import cluster.actor.monitor.ClusterEventMoniter
import com.JavaTest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sdspray.domain.ClusterNode
import spray.routing.{Route, Directives, HttpService, RequestContext}
import java.io.File
import org.parboiled.common.FileUtils

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import spray.routing.directives.CachingDirectives
import spray.can.server.Stats
import spray.can.Http
import spray.httpx.marshalling.Marshaller
import spray.httpx.encoding.Gzip
import spray.util._
import spray.http._
import MediaTypes._

/**
  * Created by LENOVO on 2016/9/13.
  */
class MonitorService(implicit actorRefFactory: ActorSystem) extends Directives {


  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  val test= new JavaTest()


  val  test1= new JavaTest()

  val monitorRoutes = {
    get {
       path("clusterStatus") {
        complete {
          sendClusterStatuResponse
        }

      }
    } ~
      (post | parameter('method ! "post")) {
        path("stop") {
          complete {
            /*in(1.second) {
              actorSystem.shutdown()
            }*/
            "Shutting down in 1 second..."
          }
        }
      }
  }

  def sendClusterStatuResponse() = {

    import scala.collection._
    actorRefFactory.actorSelection("/user/master/" + MasterActor.MONITERACTORNAME)
      .ask(ClusterEventMoniter.GETCLUSTERSTATE)(2.second)
      .mapTo[mutable.Set[Member]]

  }

  import scala.collection._

  implicit val clusterStatueMarshaller: Marshaller[mutable.Set[Member]] =
    Marshaller.delegate[mutable.Set[Member], String](ContentTypes.`application/json`) { clusterState =>


      import collection.JavaConversions._
      val  _set:util.Set[ClusterNode]=clusterState.map(member=>{

        ClusterNode(member.uniqueAddress.address.toString,member.getRoles)

      })

      new Gson().toJson(_set)
    }


}
