package cluster.factorial

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, ReceiveTimeout}
import akka.cluster.Cluster
import akka.cluster.routing.{ClusterRouterPool, AdaptiveLoadBalancingPool, SystemLoadAverageMetricsSelector, ClusterRouterPoolSettings}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

//#frontend
class FactorialFrontend(upToN: Int, repeat: Boolean) extends Actor with ActorLogging {

  val backend = context.actorOf(FromConfig.props(),
    name = "factorialBackendRouter")

  /*val backend = context.actorOf(
    ClusterRouterPool(AdaptiveLoadBalancingPool(
      SystemLoadAverageMetricsSelector), ClusterRouterPoolSettings(
      totalInstances = 100, maxInstancesPerNode = 3,
      allowLocalRoutees = false, useRole = Some("backend"))).props(Props[FactorialBackend]),
    name = "factorialBackendRouter2")*/

  override def preStart(): Unit = {
    sendJobs()
    if (repeat) {
      context.setReceiveTimeout(10.seconds)
    }
  }

  def receive = {
    case (n: Int, factorial: BigInt) =>
      if (n == upToN) {
        log.debug("{}! = {}", n, factorial)
        if (repeat) sendJobs()
        else context.stop(self)
      }
    case ReceiveTimeout =>
      log.info("Timeout")
      sendJobs()
  }

  def sendJobs(): Unit = {
    log.info("Starting batch of factorials up to [{}]", upToN)
    1 to upToN foreach {
      backend ! _
    }
  }
}

//#frontend

object FactorialFrontend {
  def main(args: Array[String]): Unit = {
    val upToN = 200

    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
      withFallback(ConfigFactory.load("factorial"))

    val system = ActorSystem("ClusterSystem", config)
    system.log.info("Factorials will start when 2 backend members in the cluster.")
    //#registerOnUp
    Cluster(system) registerOnMemberUp {
      system.actorOf(Props(classOf[FactorialFrontend], upToN, true),
        name = "factorialFrontend")
    }
    //#registerOnUp


  }
}
