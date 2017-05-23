import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonOps {

  implicit val nodeWrites: Writes[Node] = (
    (__ \ "id").writeNullable[Int] and
      (__ \ "id_string").writeNullable[String] and
      (__ \ "host").writeNullable[String] and
      (__ \ "port").writeNullable[Int] and
      (__ \ "rack").writeNullable[String]
    ) (unlift(Node.unapply))

  implicit val partitionAssignmentStateWrites: Writes[PartitionAssignmentState] = (
    (__ \ "group").write[String] and
      (__ \ "coordinator").writeNullable[Node] and
      (__ \ "topic").writeNullable[String] and
      (__ \ "partition").writeNullable[Int] and
      (__ \ "offset").writeNullable[Long] and
      (__ \ "lag").writeNullable[Long] and
      (__ \ "consumer_id").writeNullable[String] and
      (__ \ "host").writeNullable[String] and
      (__ \ "client_id").writeNullable[String] and
      (__ \ "log_end_offset").writeNullable[Long]
    ) (unlift(PartitionAssignmentState.unapply))

  implicit val groupInfoWrites: Writes[GroupInfo] = (
    (__ \ "state").writeNullable[String] and
      (__ \ "partition_assignment").writeNullable[Seq[PartitionAssignmentState]]
    ) (unlift(GroupInfo.unapply))

}
