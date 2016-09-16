package cluster.actor.backend

import akka.actor.{Actor, ActorLogging}
import cluster.message.message2kafka
import kafka.javaapi.producer.Producer
import org.slf4j.{LoggerFactory, Logger}


/**
  * Created by LENOVO on 2016/8/30.
  */
class KafkaProduceActor(val producer: Producer[Array[Byte], Array[Byte]]) extends Actor with ActorLogging {


  def receive = {
    case message2kafka(kafkaMessage) => {
      producer.send(kafkaMessage)
      log.info("send message {} to kafka ", kafkaMessage.toString)
    }

    case _ =>


  }

}
