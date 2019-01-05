
package object models {

  case class Health(healthy: Boolean, message: String, error: Option[Throwable])

  case class GroupInfo(state: Option[String] = None, partitionAssignmentStates: Option[Seq[PartitionAssignmentState]] = None, lagPerTopic: Option[Map[String, Long]] = None)

  object Node {
    def from(n: org.apache.kafka.common.Node): Node = Node(Some(n.id), Some(n.idString), Some(n.host), Some(n.port), Some(n.rack))
  }
  case class Node(id: Option[Int] = None, idString: Option[String] = None, host: Option[String] = None, port: Option[Int] = None, rack: Option[String] = None)

  case class KafkaClusterHealthResponse(clusterId: String, controller: Node, nodes: Seq[Node])

  //This is a copy of the object inside the KafkaConsumerGroupService which is protected
  case class PartitionAssignmentState(group: String, coordinator: Option[Node] = None, topic: Option[String] = None,
                                      partition: Option[Int] = None, offset: Option[Long] = None, lag: Option[Long] = None,
                                      consumerId: Option[String] = None, host: Option[String] = None,
                                      clientId: Option[String] = None, logEndOffset: Option[Long] = None)


  case class RegistryKafkaMetric(prefix: String, topic: String, partition: Option[String], group: String, suffix: String)

  object RegistryKafkaMetric {

    val metricRegex = "(.+)\\.(.+)\\.(.+)\\.(.+)\\.(.+)".r
    val metricWithoutPartitionRegex = "(.+)\\.(.+)\\.(.+)\\.(.+)".r

    def encode(metric: RegistryKafkaMetric): String = metric.partition match {
      case Some(partition) => s"${removeDots(metric.prefix)}." +
        s"${removeDots(metric.topic)}." +
        s"${removeDots(partition)}." +
        s"${removeDots(metric.group)}." +
        s"${removeDots(metric.suffix)}"
      case None => s"${removeDots(metric.prefix)}" +
        s".${removeDots(metric.topic)}" +
        s".${removeDots(metric.group)}" +
        s".${removeDots(metric.suffix)}"
    }

    private def removeDots(s: String) = s.replaceAll("\\.", "_")

    def decode(metricName: String): Option[RegistryKafkaMetric] = metricName match {
      case metricRegex(prefix, topic, partition, group, suffix) => Some(RegistryKafkaMetric(prefix, topic, Some(partition), group, suffix))
      case metricWithoutPartitionRegex(prefix, topic, group, suffix) => Some(RegistryKafkaMetric(prefix, topic, None, group, suffix))
      case _ => None
    }

  }

}
