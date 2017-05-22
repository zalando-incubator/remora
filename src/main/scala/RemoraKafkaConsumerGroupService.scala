import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.codahale.metrics.MetricRegistry
import kafka.admin.ConsumerGroupCommand
import kafka.admin.ConsumerGroupCommand.{ConsumerGroupCommandOptions, KafkaConsumerGroupService}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class GroupInfo(state: Option[String] = None, partitionAssignmentStates: Option[Seq[PartitionAssignmentState]] = None)

case class Node(id: Option[Int] = None, idString: Option[String] = None, host: Option[String] = None, port: Option[Int] = None, rack: Option[String] = None)

//This is a copy of the object inside the KafkaConsumerGroupService which is protected
case class PartitionAssignmentState(group: String, coordinator: Option[Node] = None, topic: Option[String] = None,
                                    partition: Option[Int] = None, offset: Option[Long] = None, lag: Option[Long] = None,
                                    consumerId: Option[String] = None, host: Option[String] = None,
                                    clientId: Option[String] = None, logEndOffset: Option[Long] = None)

class RemoraKafkaConsumerGroupService(kafkaSettings: KafkaSettings)
                                     (implicit executionContext: ExecutionContextExecutor, metricRegistry: MetricRegistry) {

  private val logger = Logger.getLogger(RemoraKafkaConsumerGroupService.this.getClass.getName)

  private val listTimerName = "list-timer"
  private val describeTimerName = "describe-timer"

  def measure[T](name: String)(func: => T): T = {
    val startTime = System.currentTimeMillis()
    val retval = func
    val duration: Long = System.currentTimeMillis() - startTime
    timeBlock(name, duration)
    retval
  }

  private def timeBlock(name: String, duration: Long) = {
    metricRegistry.timer(name).update(duration, TimeUnit.MILLISECONDS)
  }

  private def createKafkaConsumerGroupService(groupId: Option[String] = None): ConsumerGroupCommand.KafkaConsumerGroupService = {

    val baseConfig: Array[String] = Array("--bootstrap-server", kafkaSettings.address)

    groupId match {
      case Some(g) => createKafkaConsumerGroupService(baseConfig ++ Array("--group", g))
      case None => createKafkaConsumerGroupService(baseConfig)
    }
  }

  def createKafkaConsumerGroupService(baseConfig: Array[String]): KafkaConsumerGroupService = {
    new KafkaConsumerGroupService(new ConsumerGroupCommandOptions(baseConfig))
  }

  def list(): Future[List[String]] = Future {
    measure(listTimerName) {
      val adminClient = createKafkaConsumerGroupService()
      try {
        adminClient.listGroups()
      } finally {
        adminClient.close()
      }
    }
  }

  def describeConsumerGroup(group: String): Future[GroupInfo] = Future {

    measure(describeTimerName) {
      val kafkaConsumerGroupService = createKafkaConsumerGroupService(Some(group))
      try {
        val (state, assignments) = kafkaConsumerGroupService.describeGroup()
        assignments match {
          case Some(partitionAssignmentStates) => GroupInfo(state,
            Some(partitionAssignmentStates.map(a => PartitionAssignmentState(a.group,
              a.coordinator match {
                case Some(c) => Some(Node(Option(c.id), Option(c.idString), Option(c.host), Option(c.port), Option(c.rack)))
                case None => None
              },
              a.topic, a.partition, a.offset,
              a.lag, a.consumerId, a.host, a.clientId, a.logEndOffset))))
          case None => GroupInfo(state)
        }
      } finally {
        kafkaConsumerGroupService.close()
      }
    }
  }
}