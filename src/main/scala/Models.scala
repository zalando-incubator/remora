

case class Health(healthy: Boolean, message: String, error: Option[Throwable])

case class GroupInfo(state: Option[String] = None, partitionAssignmentStates: Option[Seq[PartitionAssignmentState]] = None, lagPerTopic: Option[Map[String, Long]] = None)

case class Node(id: Option[Int] = None, idString: Option[String] = None, host: Option[String] = None, port: Option[Int] = None, rack: Option[String] = None)

//This is a copy of the object inside the KafkaConsumerGroupService which is protected
case class PartitionAssignmentState(group: String, coordinator: Option[Node] = None, topic: Option[String] = None,
                                    partition: Option[Int] = None, offset: Option[Long] = None, lag: Option[Long] = None,
                                    consumerId: Option[String] = None, host: Option[String] = None,
                                    clientId: Option[String] = None, logEndOffset: Option[Long] = None)


