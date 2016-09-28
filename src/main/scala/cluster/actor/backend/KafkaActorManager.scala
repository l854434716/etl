package cluster.actor.backend

import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import cluster.message.message2kafka
import kafka.javaapi.producer.Producer
import kafka.producer.ProducerConfig

/**
  * Created by LENOVO on 2016/8/30.
  */
class KafkaActorManager extends Actor with ActorLogging {

  val props = new Properties()
  props.put("metadata.broker.list", "192.168.2.223:9092,192.168.2.224:9092,192.168.2.225:9092");
  // props.put("serializer.class", "kafka.serializer.StringEncoder");
  props.put("key.serializer.class", "kafka.serializer.DefaultEncoder");
  //		props.put("partitioner.class",
  //				"test.kafka.examples.one.SimplePartitioner1");
  props.put("request.required.acks", "1");

  val config: ProducerConfig = new ProducerConfig(props);

  val producer: Producer[Array[Byte], Array[Byte]] = new Producer[Array[Byte], Array[Byte]](config)

  val kafkaProducerActors = context.actorOf(RoundRobinPool(5).props(Props(classOf[KafkaProduceActor], producer)), "kafkaAcors")


  override def postStop(): Unit = {
    producer.close
  }

  override def receive: Receive = {
    case message2kafka(kafkaMessage) => kafkaProducerActors.tell(message2kafka, ActorRef.noSender)
    case _ =>
  }
}
