package cluster.startup

/**
  * Created by LENOVO on 2016/8/31.
  */
object Content {


  val PROJECT_HOME = ProjectHome.projectHome


  val DEFAULT_AKKA_CONF_PATH = PROJECT_HOME.concat("/conf/akka/application.conf")
  val DEFAULT_SPRAY_CONF_PATH = PROJECT_HOME.concat("/conf/akka/spray.conf")


  val COLLECTORROLENAME = "collector"

  val MASTERROLENAME = "master"

  val BACKENDROLENAME = "backend"


  val LOGCONFIGFLIE = PROJECT_HOME.concat("/conf/client/log.properties")

}


object ProjectHome {

  var projectHome: String = _

  def setHome(isRuninIDE: Boolean) = {

    if (isRuninIDE) {
      projectHome = ".";
    } else {
      projectHome = "..";
    }
  }

}
