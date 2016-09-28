package cluster.message

import kafka.producer.KeyedMessage

/**
  * Created by LENOVO on 2016/8/30.
  */
case class message2kafka(message: KeyedMessage[Array[Byte], Array[Byte]])