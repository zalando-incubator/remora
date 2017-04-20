import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.codahale.metrics.MetricRegistry
import kafka.admin.AdminClient
import kafka.admin.ConsumerGroupCommand.LogEndOffsetResult
import kafka.common.TopicAndPartition
import kafka.coordinator.GroupOverview
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/*based on kafka.admin.ConsumerGroupCommand which gives an example of the below
*
* ➜  bin ./kafka-run-class.sh kafka.admin.ConsumerGroupCommand --new-consumer --describe --bootstrap-server localhost:9092 --group console-consumer-96416
GROUP                          TOPIC                          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             OWNER
console-consumer-96416         test                           0          3               3               0               consumer-1_/192.168.0.102
console-consumer-96416         test                           1          4               4               0               consumer-1_/192.168.0.102
*
* */

case class GroupInfo(group: String, topic: String, partition: Int, offset: Option[Long] = None, logEndOffset: Option[Long] = None, lag: Option[Long] = None, owner: Option[String] = None)

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

  private def createAdminClient(): AdminClient = {
    logger.info("Creating admin client")
    val props = new Properties()
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaSettings.address)
    AdminClient.create(props)
  }

  private def createNewConsumer(group: String): KafkaConsumer[String, String] = {
    val properties = new Properties()
    val deserializer = (new StringDeserializer).getClass.getName
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaSettings.address)
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, group)
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer)
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer)

    new KafkaConsumer(properties)
  }

  def list(): Future[List[GroupOverview]] = Future {
    measure(listTimerName) {
      val adminClient = createAdminClient()
      try {
        adminClient.listAllConsumerGroupsFlattened()
      } finally {
        adminClient.close()
      }
    }
  }

  def describeConsumerGroup(group: String): Future[List[GroupInfo]] = measure(describeTimerName) {

    logger.info(s"Describing group $group via admin client")
    val adminClient = createAdminClient()
    val consumerSummaries = Future {
      try {
        adminClient.describeConsumerGroup(group)
      } finally {
        adminClient.close()
      }
    }

    consumerSummaries.map {
      _.flatMap { consumerSummary =>

        implicit val consumer = createNewConsumer(group)

        try {
          val topicPartitions = consumerSummary.assignment.map(tp => TopicAndPartition(tp.topic, tp.partition))

          val partitionOffsets = topicPartitions.flatMap { topicPartition =>
            logger.info(s"Querying kafka on consumer")

            Option(consumer.committed(new TopicPartition(topicPartition.topic, topicPartition.partition))).map { offsetAndMetadata =>
              topicPartition -> offsetAndMetadata.offset
            }
          }.toMap

          topicPartitions.sortBy { case topicPartition => topicPartition.partition }

          describeTopicPartition(group, topicPartitions, partitionOffsets.get, _ => Some(s"${consumerSummary.clientId}_${consumerSummary.clientHost}"))
        } finally {
          consumer.close()
        }
      }
    }

  }

  private def describeTopicPartition(group: String,
                                     topicPartitions: Seq[TopicAndPartition],
                                     getPartitionOffset: TopicAndPartition => Option[Long],
                                     getOwner: TopicAndPartition => Option[String])
                                    (implicit consumer: KafkaConsumer[String, String]): Seq[GroupInfo] = {
    topicPartitions
      .sortBy { case topicPartition => topicPartition.partition }
      .flatMap { topicPartition =>
        describePartition(group, topicPartition.topic, topicPartition.partition, getPartitionOffset(topicPartition),
          getOwner(topicPartition))
      }
  }

  private def describePartition(group: String,
                                topic: String,
                                partition: Int,
                                offsetOpt: Option[Long],
                                ownerOpt: Option[String])
                               (implicit consumer: KafkaConsumer[String, String]): Option[GroupInfo] = {
    def convert(logEndOffset: Option[Long]): GroupInfo = {
      val lag = offsetOpt.filter(_ != -1).flatMap(offset => logEndOffset.map(_ - offset))
      GroupInfo(group, topic, partition, offsetOpt, logEndOffset, lag, ownerOpt)
    }
    getLogEndOffset(topic, partition) match {
      case LogEndOffsetResult.LogEndOffset(logEndOffset) => Some(convert(Some(logEndOffset)))
      case LogEndOffsetResult.Unknown => None
      case LogEndOffsetResult.Ignore => None
    }
  }

  protected def getLogEndOffset(topic: String, partition: Int)
                               (implicit consumer: KafkaConsumer[String, String]): LogEndOffsetResult = {
    val topicPartition = new TopicPartition(topic, partition)
    consumer.assign(List(topicPartition).asJava)
    consumer.seekToEnd(List(topicPartition).asJava)
    val logEndOffset = consumer.position(topicPartition)
    LogEndOffsetResult.LogEndOffset(logEndOffset)
  }
}