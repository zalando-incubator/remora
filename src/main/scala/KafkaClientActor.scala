import KafkaClientActor.{DescribeKafkaCluster, DescribeKafkaConsumerGroup, ListConsumers}
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import kafka.admin.RemoraKafkaConsumerGroupService
import nl.grons.metrics4.scala.{ActorInstrumentedLifeCycle, ReceiveCounterActor, ReceiveExceptionMeterActor, ReceiveTimerActor}

object KafkaClientActor {

  sealed trait Command

  case class DescribeKafkaConsumerGroup(consumerGroupName: String) extends Command
  object DescribeKafkaCluster extends Command
  object ListConsumers extends Command

  def props(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) = Props(new KafkaClientActor(kafkaConsumerGroupService))

}

class BaseKafkaClientActor(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService) extends Actor with ActorLogging
  with nl.grons.metrics4.scala.DefaultInstrumented with ActorInstrumentedLifeCycle {

  import context.dispatcher

  def receive: Receive = {
    case DescribeKafkaCluster =>
      log.info(s"Received request for cluster description")
      kafkaConsumerGroupService.describeCluster() pipeTo sender()
    case DescribeKafkaConsumerGroup(consumerGroupName) =>
      log.info(s"Received request for $consumerGroupName")
      kafkaConsumerGroupService.describeConsumerGroup(consumerGroupName) pipeTo sender()
    case ListConsumers =>
      log.info(s"Received request for consumer list")
      kafkaConsumerGroupService.list() pipeTo sender()
  }
}

class KafkaClientActor(kafkaConsumerGroupService: RemoraKafkaConsumerGroupService)
  extends BaseKafkaClientActor(kafkaConsumerGroupService) with ReceiveCounterActor with ReceiveTimerActor with ReceiveExceptionMeterActor
