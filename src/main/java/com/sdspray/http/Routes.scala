package com.sdspray.http

import com.sdspray.core.AbstractAkkaSystem
import akka.actor.Props
import com.sdspray.http.service.MonitorService
import spray.httpx.encoding.Gzip
import spray.routing._
import spray.http.StatusCodes._
import spray.routing.directives.LogEntry
import akka.event.Logging._
import spray.http.HttpRequest
import com.sdspray.actors.{IndexService}

/**
  * Created by JiangFeng on 2014/4/25.
  */
trait Routes extends RouteConcatenation with StaticRoute with AbstractAkkaSystem {

  val httpServer = system.actorOf(Props(classOf[HttpServer], allRoutes))

  implicit def actorRefFactory = system


  lazy val allRoutes = logRequest(showReq _) {
    new IndexService().route ~ new MonitorService().monitorRoutes ~ staticRoute
  }

  //  implicit val wsocketServer: WSocketServer
  //  wsocketServer.forResource("/ws", Some(index))


  private def showReq(req: HttpRequest) = LogEntry(req.uri, InfoLevel)
}


trait StaticRoute extends Directives {
  this: AbstractAkkaSystem =>

  lazy val staticRoute =
    path("favicon.ico") {
      getFromResource("favicon.ico")
    } ~
      pathPrefix("markers") {
        getFromResourceDirectory("markers/")
      } ~
      pathPrefix("css") {
        getFromResourceDirectory("css/")
      } ~
      pathEndOrSingleSlash {
        getFromResource("index.scala")
      } ~ complete(NotFound)
}

trait DemoService extends HttpService {

  import java.io.File
  import org.parboiled.common.FileUtils
  import scala.concurrent.duration._
  import akka.actor._
  import akka.pattern.ask
  import spray.routing.{HttpService, RequestContext}
  import spray.routing.directives.CachingDirectives
  import spray.can.server.Stats
  import spray.can.Http
  import spray.httpx.marshalling.Marshaller
  import spray.httpx.encoding.Gzip
  import spray.util._
  import spray.http._
  import MediaTypes._
  import CachingDirectives._

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  val demoRoute = {
    get {
      pathSingleSlash {
        complete(index)
      } ~
        path("ping") {
          complete("PONG!")
        } ~
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
            actorRefFactory.actorFor("/user/IO-HTTP/listener-0")
              .ask(Http.GetStats)(1.second)
              .mapTo[Stats]
          }
        } ~
        path("timeout") { ctx =>
          // we simply let the request drop to provoke a timeout
        } ~
        path("crash") { ctx =>
          sys.error("crash boom bang")
        } ~
        path("fail") {
          failWith(new RuntimeException("aaaahhh"))
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


  lazy val index =
    <html>
      <body>
        <h1>Say hello to
          <i>spray-routing</i>
          on
          <i>spray-can</i>
          !</h1>
        <p>Defined resources:</p>
        <ul>
          <li>
            <a href="/ping">/ping</a>
          </li>
          <li>
            <a href="/stream1">/stream1</a>
            (via a Stream[T])</li>
          <li>
            <a href="/stream2">/stream2</a>
            (manually)</li>
          <li>
            <a href="/stream-large-file">/stream-large-file</a>
          </li>
          <li>
            <a href="/stats">/stats</a>
          </li>
          <li>
            <a href="/timeout">/timeout</a>
          </li>
          <li>
            <a href="/cached">/cached</a>
          </li>
          <li>
            <a href="/crash">/crash</a>
          </li>
          <li>
            <a href="/fail">/fail</a>
          </li>
          <li>
            <a href="/stop?method=post">/stop</a>
          </li>
        </ul>
      </body>
    </html>

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
}



