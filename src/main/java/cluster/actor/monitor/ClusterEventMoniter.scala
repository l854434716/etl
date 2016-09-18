package cluster.actor.monitor

import akka.actor.{ActorLogging, RootActorPath, Actor}
import akka.cluster.{MemberStatus, Member, Cluster}
import akka.cluster.ClusterEvent._
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * 监控集群事件，监控各节点的状态
  */
class ClusterEventMoniter extends Actor with ActorLogging {

  import ClusterEventMoniter._

  val cluster = Cluster(context.system)


  val activeMembers= mutable.Set.empty[Member]

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[ClusterMetricsChanged], classOf[UnreachableMember], classOf[MemberEvent])
  }

  def receive = {

    case MemberUp(member) =>
      doMemberUp(member)

    case UnreachableMember(member) =>
      doMemberUnreachable(member)

    case MemberRemoved(member, previousStatus) =>
      doMemberRemoved(member, previousStatus)

    case state: CurrentClusterState =>
      doCurrentClusterState(state)
    case GETCLUSTERSTATE => {
      sender() ! activeMembers
    }
    case _ => //igron
  }

  private def doMemberUp(member: Member) = {
    log.info("node address is {} with roles is {}  memberup in cluster",
      member.address.toString, member.getRoles.toString)
    activeMembers+=member
  }

  private def doMemberRemoved(member: Member, previousStatus: MemberStatus) = {
    log.info("node address is {} with roles is {} remove from   cluster",
      member.address.toString, member.getRoles.toString)
    activeMembers-=member
  }

  private def doMemberUnreachable(member: Member) = {

    log.info("node address is {} with roles is {}  unrechable",
      member.address.toString, member.getRoles.toString)
    activeMembers-=member
  }

  private def doCurrentClusterState(state: CurrentClusterState) = {
    activeMembers++=state.members
  }

  private def findActorByMemberAndPath(member: Member, path: String) = {

    context.actorSelection(RootActorPath(member.address) + path)

  }


}

object ClusterEventMoniter {

  case object GETCLUSTERSTATE

}


