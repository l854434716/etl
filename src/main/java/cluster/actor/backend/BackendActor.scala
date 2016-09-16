package cluster.actor.backend

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.routing.{AdaptiveLoadBalancingGroup, ClusterRouterGroup, ClusterRouterGroupSettings, HeapMetricsSelector}
import cluster.actor.collector.MoniterMessage
import cluster.message.message2kafka
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * Created by LENOVO on 2016/8/30.
  */
class BackendActor extends Actor with ActorLogging {

  val logger = LoggerFactory.getLogger(classOf[BackendActor])

  val selfAddress = Cluster(context.system).selfAddress

  val kafkaActorManager = context.actorOf(Props[KafkaActorManager], "kafkaActorManager")

  val cluster = Cluster(context.system)

  /*val master = context.actorOf(
    ClusterRouterGroup(AdaptiveLoadBalancingGroup(HeapMetricsSelector),
      ClusterRouterGroupSettings(
        totalInstances = 5, routeesPaths = List("/user/master"),
        allowLocalRoutees = true, useRole = Some("master"))).props(),
    name = "masterRouter")*/
  var i = 0

  def receive = {
    case message2kafka(kafkaMessage) => {

      i = i + 1
      log.info("backend get message form  collector  " + i)
      kafkaActorManager ! message2kafka

    }

    case _ => //todo
  }


  /*import context.dispatcher
  context.system.scheduler.schedule(30.seconds ,60.seconds)({
    master ! moniter
    logger.info("send  heap info")
  })*/

}
