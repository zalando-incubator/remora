package kafka.admin

import java.util.logging.Logger

import config.KafkaSettings
import kafka.admin.ConsumerGroupCommand.ConsumerGroupCommandOptions
import models.{GroupInfo, Node, PartitionAssignmentState}
import org.apache.kafka.clients.admin

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}

trait ConsumerGroupService {
  def list(): Future[List[String]]

  def describeCluster(): Future[admin.DescribeClusterResult]
  def describeConsumerGroup(group: String): Future[GroupInfo]
}

class RemoraKafkaConsumerGroupService(kafkaSettings: KafkaSettings)
                                     (implicit executionContext: ExecutionContextExecutor) extends ConsumerGroupService
  with nl.grons.metrics.scala.DefaultInstrumented {

  private val logger = Logger.getLogger(RemoraKafkaConsumerGroupService.this.getClass.getName)

  private val listTimer = metrics.timer("list-timer")
  private val describeTimer = metrics.timer("describe-timer")
  private val describeGroupTimer = metrics.timer("describe-group-timer")

  private def createKafkaConsumerGroupService(groupId: Option[String] = None): ConsumerGroupCommand.ConsumerGroupService = {
    groupId match {
      case Some(g) => createKafkaConsumerGroupService(baseConfig() ++ Array("--group", g))
      case None => createKafkaConsumerGroupService(baseConfig())
    }
  }

  private def baseConfig(): Array[String] = {
    var baseConfig: ArrayBuffer[String] = ArrayBuffer("--bootstrap-server", kafkaSettings.address)

    if (!kafkaSettings.commandConfig.isEmpty) {
      baseConfig ++= Array("--command-config", kafkaSettings.commandConfig)
    }

    baseConfig.toArray
  }

  def createKafkaConsumerGroupService(consumerGroupCommandArgs: Array[String]): ConsumerGroupCommand.ConsumerGroupService = {
    new ConsumerGroupCommand.ConsumerGroupService(new ConsumerGroupCommandOptions(consumerGroupCommandArgs))
  }

  override def describeCluster(): Future[admin.DescribeClusterResult] = Future {
    describeTimer.time {
      kafkaSettings.adminClient.describeCluster()
    }
  }

  override def list(): Future[List[String]] = Future {
    listTimer.time {
      val groupService = createKafkaConsumerGroupService()
      try {
        groupService.listGroups()
      } finally {
        groupService.close()
      }
    }
  }

  override def describeConsumerGroup(group: String): Future[GroupInfo] = Future {
    describeGroupTimer.time {
      val kafkaConsumerGroupService = createKafkaConsumerGroupService(Some(group))
      try {
        val (state, assignments) = kafkaConsumerGroupService.collectGroupOffsets()
        assignments match {
          case Some(partitionAssignmentStates) =>
            val assignments = Some(partitionAssignmentStates.map(a => PartitionAssignmentState(a.group,
              a.coordinator match {
                case Some(c) => Some(Node(Option(c.id), Option(c.idString), Option(c.host), Option(c.port), Option(c.rack)))
                case None => None
              },
              a.topic, a.partition, a.offset,
              a.lag, a.consumerId, a.host, a.clientId, a.logEndOffset)))

            val lagPerTopic = Some(partitionAssignmentStates.filter(state => state.topic.isDefined)
              .groupBy(state => state.topic.get)
              .map { case (topic, partitions) => (topic, partitions.map(_.lag).map {
                case Some(lag) => lag.toLong
                case None => 0L
              }.sum)
              })

            GroupInfo(state, assignments, lagPerTopic)
          case None => GroupInfo(state)
        }
      } finally {
        kafkaConsumerGroupService.close()
      }
    }
  }
}