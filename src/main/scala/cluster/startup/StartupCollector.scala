package cluster.startup

import java.io.File

import akka.actor.{Props, ActorSystem}
import cluster.actor.collector.CollectorActor
import com.typesafe.config.ConfigFactory

/**
  * Created by LENOVO on 2016/9/1.
  */
object StartupCollector {


  var runningInEclipse = true;
  val defaultConfPath = "../conf";
  // 默认的conf 所在的相对路径
  val file = new File(defaultConfPath);

  if (file.exists()) {
    runningInEclipse = false;
  } else {
    runningInEclipse = true;

  }
  ProjectHome.setHome(runningInEclipse)

  val role = Content.COLLECTORROLENAME

  val port: Int = 0

  var system: ActorSystem = null

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [$role]")).
    withFallback(ConfigFactory.parseFile(new File(Content.DEFAULT_AKKA_CONF_PATH)))


  def startCollectorAkkaSystem(): ActorSystem = {
    this.synchronized {

      if (system != null)
        return system
      system = ActorSystem("ClusterSystem", config)
      system.actorOf(Props[CollectorActor], name = Content.COLLECTORROLENAME)
      system
    }

  }


}


object Main1 extends App {

  StartupCollector.startCollectorAkkaSystem();
}
