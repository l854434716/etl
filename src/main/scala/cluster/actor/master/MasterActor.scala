package cluster.actor.master

import akka.actor.{Props, Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import cluster.actor.collector.MoniterMessage
import cluster.actor.monitor.ClusterEventMoniter


/**
  * Created by luozhi on 2016/8/17.
  */
class MasterActor extends Actor with ActorLogging {

  import MasterActor._

  val clouser = Cluster(context.system)


  val moniterActor = context.actorOf(Props[ClusterEventMoniter], MONITERACTORNAME)

  def receive = {

    case _ =>
  }

}

object MasterActor {
  val MONITERACTORNAME = "monitorActor"
}
