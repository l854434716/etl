package cluster.actor.collector

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.routing.{AdaptiveLoadBalancingGroup, ClusterRouterGroup, ClusterRouterGroupSettings, HeapMetricsSelector}
import cluster.message.message2kafka
import kafka.producer.KeyedMessage

import scala.concurrent.duration._

case class MoniterMessage(text: String)

/**
  * Created by luozhi on 2016/8/17.
  */
class CollectorActor extends Actor with ActorLogging {

  val selfAddress = Cluster(context.system).selfAddress

  val cluster = Cluster(context.system)


  /*val master = context.actorOf(
    ClusterRouterGroup(AdaptiveLoadBalancingGroup(HeapMetricsSelector),
      ClusterRouterGroupSettings(
        totalInstances = 5, routeesPaths = List("/user/master"),
        allowLocalRoutees = true, useRole = Some("master"))).props(),
    name = "masterRouter")*/


  val backend = context.actorOf(
    ClusterRouterGroup(AdaptiveLoadBalancingGroup(HeapMetricsSelector),
      ClusterRouterGroupSettings(
        totalInstances = 5, routeesPaths = List("/user/backend"),
        allowLocalRoutees = true, useRole = Some("backend"))).props(),
    name = "backendRouter")

  /*// Subscribe unto ClusterMetricsEvent events.
  override def preStart(): Unit = extension.subscribe(self,classOf[ClusterMetricsChanged])

  // Unsubscribe from ClusterMetricsEvent events.
  override def postStop(): Unit = extension.unsubscribe(self)*/

  def receive = {
    case state: CurrentClusterState => // Ignore.

    case keyMessage: KeyedMessage[Array[Byte], Array[Byte]] => {

      backend ! message2kafka(keyMessage)

      log.info("accept  keymessage")
    }

    case a: String => log.info(a)
    case _ => {}
  }


}
