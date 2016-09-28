package cluster.factorial

//#metrics-listener
import akka.actor.{Actor, ActorLogging}
import akka.cluster.StandardMetrics.{Cpu, HeapMemory}
import akka.cluster.{NodeMetrics, Cluster}
import akka.cluster.ClusterEvent.{ClusterMetricsChanged, CurrentClusterState}

class MetricsListener extends Actor with ActorLogging {
  val selfAddress = Cluster(context.system).selfAddress

  // Subscribe unto ClusterMetricsEvent events.
  override def preStart(): Unit = Cluster(context.system).subscribe(self, classOf[ClusterMetricsChanged])

  // Unsubscribe from ClusterMetricsEvent events.
  override def postStop(): Unit = Cluster(context.system).unsubscribe(self)

  def receive = {
    case ClusterMetricsChanged(clusterMetrics) =>
      clusterMetrics.filter(_.address == selfAddress) foreach { nodeMetrics =>
        logHeap(nodeMetrics)
        logCpu(nodeMetrics)
      }
    case state: CurrentClusterState => // Ignore.
  }

  def logHeap(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case HeapMemory(address, timestamp, used, committed, max) =>
      log.info("Used heap: {} MB", used.doubleValue / 1024 / 1024)
    case _ => println("no  heap  info")
  }

  def logCpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(address, timestamp, Some(systemLoadAverage), cpuCombined, processors) =>
      log.info("Load: {} ({} processors)", systemLoadAverage, processors)
    case _ => println("No cpu info") // No cpu info.
  }
}

//#metrics-listener
