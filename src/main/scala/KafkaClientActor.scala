import KafkaClientActor.{DescribeKafkaClusterConsumer, ListConsumers}
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import nl.grons.metrics.scala.{ReceiveExceptionMeterActor, ReceiveTimerActor, ReceiveCounterActor, ActorInstrumentedLifeCycle}

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaClusterConsumer(consumerGroupName: String) extends Command
  object ListConsumers extends Command

  def props(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) = Props(classOf[KafkaClientActor], kafkaConsumerGroupService)

}

class BaseKafkaClientActor(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) extends Actor with ActorLogging
with nl.grons.metrics.scala.DefaultInstrumented with ActorInstrumentedLifeCycle {

  import context.dispatcher

  def receive: Receive = {

    case DescribeKafkaClusterConsumer(consumerGroupName) =>
      log.info(s"Received request for $consumerGroupName")
      kafkaConsumerGroupService.describeConsumerGroup(consumerGroupName) pipeTo sender
    case ListConsumers =>
      log.info(s"Received request for consumer list")
      kafkaConsumerGroupService.list() pipeTo sender
  }
}

class KafkaClientActor(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService)
  extends BaseKafkaClientActor(kafkaConsumerGroupService) with ReceiveCounterActor with ReceiveTimerActor with ReceiveExceptionMeterActor
