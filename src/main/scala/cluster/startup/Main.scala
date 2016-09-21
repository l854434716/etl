package cluster.startup

import java.io.File
import java.util.Properties

import akka.actor.{ActorLogging, Props, ActorSystem}
import cluster.actor.backend.BackendActor
import cluster.actor.collector.CollectorActor
import cluster.actor.master.MasterActor
import com.sdspray.core.SprayService
import com.typesafe.config.ConfigFactory
import org.apache.log4j.PropertyConfigurator

/**
  * Created by LENOVO on 2016/8/30.
  */
object Main extends App {

  var runningInEclipse = true;
  //add by zhiluo
  // auto  get  project path
  val defaultConfPath = "../conf";
  // 默认的conf 所在的相对路径
  val file = new File(defaultConfPath);

  if (file.exists()) {
    runningInEclipse = false;
  } else {
    runningInEclipse = true;

  }
  ProjectHome.setHome(runningInEclipse)


  initLogger
  startupAkkaSystem

  def startupAkkaSystem() = {

    val role = "master" //args(0)

    val port: Int = 2552 //Integer.parseInt(args(1))


    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [$role]")).
      withFallback(ConfigFactory.parseFile(new File(Content.DEFAULT_AKKA_CONF_PATH)))

    role match {
      case "collector" => {
        val system = ActorSystem("ClusterSystem", config)
        system.actorOf(Props[CollectorActor], name = Content.COLLECTORROLENAME)
      }

      case "master" => {
        val system = ActorSystem("ClusterSystem", config)
        SprayService(system).startHttpService()
        system.actorOf(Props[MasterActor], name = Content.MASTERROLENAME)
      }

      case "backend" => {
        val system = ActorSystem("ClusterSystem", config)
        system.actorOf(Props[BackendActor], name = Content.BACKENDROLENAME)
      }
      case _ => {
        println("no role to  startup ")
      }



    }
  }


  def initLogger: Unit = {
    PropertyConfigurator.configureAndWatch(Content.LOGCONFIGFLIE)
  }
}
