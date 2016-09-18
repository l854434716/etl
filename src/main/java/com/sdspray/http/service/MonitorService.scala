package com.sdspray.http.service

import java.util

import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.Member
import cluster.actor.master.MasterActor
import cluster.actor.monitor.ClusterEventMoniter
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


  val monitorRoutes = {
    get {
      path("stream1") {
        // we detach in order to move the blocking code inside the simpleStringStream into a future
        detach() {
          respondWithMediaType(`text/html`) {
            // normally Strings are rendered to text/plain, we simply override here
            complete(simpleStringStream)
          }
        }
      } ~
        path("stream2") {
          sendStreamingResponse
        } ~
        path("stream-large-file") {
          encodeResponse(Gzip) {
            getFromFile(largeTempFile)
          }
        } ~
        path("stats") {
          complete {
            actorRefFactory.actorSelection("/user/IO-HTTP/listener-0")
              .ask(Http.GetStats)(1.second)
              .mapTo[Stats]
          }
        } ~ path("clusterStatus") {
        complete {

          sendClusterStatuResponse
        }

      }
    } ~
      (post | parameter('method ! "post")) {
        path("stop") {
          complete {
            in(1.second) {
              actorSystem.shutdown()
            }
            "Shutting down in 1 second..."
          }
        }
      }
  }

  // we prepend 2048 "empty" bytes to push the browser to immediately start displaying the incoming chunks
  lazy val streamStart = " " * 2048 + "<html><body><h2>A streaming response</h2><p>(for 15 seconds)<ul>"
  lazy val streamEnd = "</ul><p>Finished.</p></body></html>"

  def simpleStringStream: Stream[String] = {
    val secondStream = Stream.continually {
      // CAUTION: we block here to delay the stream generation for you to be able to follow it in your browser,
      // this is only done for the purpose of this demo, blocking in actor code should otherwise be avoided
      Thread.sleep(500)
      "<li>" + DateTime.now.toIsoDateTimeString + "</li>"
    }
    streamStart #:: secondStream.take(15) #::: streamEnd #:: Stream.empty
  }

  // simple case class whose instances we use as send confirmation message for streaming chunks
  case class Ok(remaining: Int)

  def sendStreamingResponse(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {
          // we use the successful sending of a chunk as trigger for scheduling the next chunk
          val responseStart = HttpResponse(entity = HttpEntity(`text/html`, streamStart))
          ctx.responder ! ChunkedResponseStart(responseStart).withAck(Ok(16))

          def receive = {
            case Ok(0) =>
              ctx.responder ! MessageChunk(streamEnd)
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)

            case Ok(remaining) =>
              in(500.millis) {
                val nextChunk = MessageChunk("<li>" + DateTime.now.toIsoDateTimeString + "</li>")
                ctx.responder ! nextChunk.withAck(Ok(remaining - 1))
              }

            case ev: Http.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }
        }
      }
    }

  implicit val statsMarshaller: Marshaller[Stats] =
    Marshaller.delegate[Stats, String](ContentTypes.`text/plain`) { stats =>
      "Uptime                : " + stats.uptime.formatHMS + '\n' +
        "Total requests        : " + stats.totalRequests + '\n' +
        "Open requests         : " + stats.openRequests + '\n' +
        "Max open requests     : " + stats.maxOpenRequests + '\n' +
        "Total connections     : " + stats.totalConnections + '\n' +
        "Open connections      : " + stats.openConnections + '\n' +
        "Max open connections  : " + stats.maxOpenConnections + '\n' +
        "Requests timed out    : " + stats.requestTimeouts + '\n'
    }

  lazy val largeTempFile: File = {
    val file = File.createTempFile("streamingTest", ".txt")
    FileUtils.writeAllText((1 to 1000) map ("This is line " + _) mkString "\n", file)
    file.deleteOnExit()
    file
  }

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    actorSystem.scheduler.scheduleOnce(duration)(body)


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
