import KafkaClientActor.{DescribeKafkaClusterConsumer, ListConsumers}
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaClusterConsumer(consumerGroupName: String) extends Command
  object ListConsumers extends Command

  def props(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) = Props(classOf[KafkaClientActor], kafkaConsumerGroupService)

}

class KafkaClientActor(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) extends Actor with ActorLogging {

  import context.dispatcher

  override def receive: Receive = {

    case DescribeKafkaClusterConsumer(consumerGroupName) =>
      log.info(s"Received request for $consumerGroupName")
      kafkaConsumerGroupService.describeConsumerGroup(consumerGroupName) pipeTo sender
    case ListConsumers =>
      log.info(s"Received request for consumer list")
      kafkaConsumerGroupService.list() pipeTo sender
  }
}
