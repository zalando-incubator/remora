
import java.util.concurrent.TimeUnit

import KafkaClientActor.{Command, DescribeKafkaClusterConsumer, ListConsumers}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.reflect.ClassTag

class Api(kafkaClientActorRef: ActorRef)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer, metricRegistry: MetricRegistry) {

  implicit val apiExecutionContext = actorSystem.dispatchers.lookup("api-dispatcher")

  implicit def remoraExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: RuntimeException =>
        extractUri { uri =>
          actorSystem.log.error(e, "Request received an exception: {}", e.getMessage)
          complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal Error! Please check logs"))
        }
    }

  implicit val duration: Timeout = 60.seconds

  val settings = ApiSettings(actorSystem.settings.config)

  val mapper = new ObjectMapper()
  mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))

  implicit val metricRegistryToEntity: ToEntityMarshaller[MetricRegistry] =
    Marshaller.ByteArrayMarshaller.wrap(MediaTypes.`application/json`)(mapper.writeValueAsBytes)

  private def askFor[RES](command: Command)(implicit tag: ClassTag[RES]) =
    (kafkaClientActorRef ? command).mapTo[RES]

  val route =
    redirectToNoTrailingSlashIfPresent(StatusCodes.Found) {
      import JsonOps._

      handleExceptions(remoraExceptionHandler) {
        path("metrics") {
          complete(metricRegistry)
        } ~ path("health") {
          complete("OK")
        } ~ pathPrefix("consumers") {
          pathEnd {
            complete(askFor[List[String]](ListConsumers).map(Json.toJson(_).toString))
          } ~ path(Segment) { consumerGroup =>
            complete(askFor[GroupInfo](DescribeKafkaClusterConsumer(consumerGroup)).map(Json.toJson(_).toString))
          }
        }
      }
    }

  def start() = Http().bindAndHandle(route, "0.0.0.0", settings.port)
}

object Api {
  def apply(kafkaClientActorRef: ActorRef)
           (implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, metricRegistry: MetricRegistry) =
    new Api(kafkaClientActorRef)
}

case class ApiSettings(port: Int)

object ApiSettings {
  def apply(config: Config): ApiSettings = ApiSettings(config.getInt("api.port"))
}

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
