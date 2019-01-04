
import java.util.concurrent.TimeUnit

import KafkaClientActor.{Command, DescribeKafkaCluster, DescribeKafkaConsumerGroup, ListConsumers}
import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route, StandardRoute}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import backline.http.metrics.{StatusCodeCounterDirectives, TimerDirectives}
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheck.Result
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import models.{GroupInfo, Health}
import org.apache.kafka.clients.admin.DescribeClusterResult
import play.api.libs.json._

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration._
import scala.reflect.ClassTag

class Api(kafkaClientActorRef: ActorRef)
         (implicit actorSystem: ActorSystem, Materializer: ActorMaterializer)
  extends StatusCodeCounterDirectives
    with LazyLogging
    with TimerDirectives
    with nl.grons.metrics.scala.DefaultInstrumented
    with PlayJsonSupport {

  implicit val apiExecutionContext: MessageDispatcher = actorSystem.dispatchers.lookup("api-dispatcher")

  implicit def remoraExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: RuntimeException =>
        extractUri { uri =>
          actorSystem.log.error(e, "Request received an exception: {}", e.getMessage)
          complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal Error! Please check logs"))
        }
    }

  val settings = ApiSettings(actorSystem.settings.config)

  implicit val timeoutDuration: Timeout = settings.timeout


  val mapper = new ObjectMapper()
  mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))

  implicit val metricRegistryToEntity: ToEntityMarshaller[MetricRegistry] =
    Marshaller.ByteArrayMarshaller.wrap(MediaTypes.`application/json`)(mapper.writeValueAsBytes)

  private def askFor[RES](command: Command)(implicit tag: ClassTag[RES]) =
    (kafkaClientActorRef ? command).mapTo[RES]

  val route: Route =
    withTimer {
      withStatusCodeCounter {
        redirectToNoTrailingSlashIfPresent(StatusCodes.Found) {
          import JsonOps._
          handleExceptions(remoraExceptionHandler) {
            path("metrics") {
              complete(metricRegistry)
            } ~ path("health") {
              healthCheck
            } ~ pathPrefix("consumers") {
              pathEnd {
                complete(askFor[List[String]](ListConsumers).map(Json.toJson(_)))
              } ~ path(Segment) { consumerGroup =>
                complete(askFor[GroupInfo](DescribeKafkaConsumerGroup(consumerGroup)).map(Json.toJson(_)))
              }
            }
          }
        }
      }
    }

  def healthCheck: StandardRoute = {
    getHealth match {
      case Health(true, message, None) => complete(message)
      case Health(false, _, Some(e)) => failWith(e)
    }
  }

  def getHealth: Health = {
    val health = healthCheck("Health") {

      val checkDuration = timeoutDuration.duration
      val healthFuture = askFor[DescribeClusterResult](DescribeKafkaCluster).map { clusterDesc =>
          val clusterNodes = clusterDesc.nodes.get(checkDuration.length, checkDuration.unit)
          val clusterId = clusterDesc.clusterId.get(checkDuration.length, checkDuration.unit)
          logger.debug(s"clusterId: $clusterId; nodes: $clusterNodes")

          val clusterIdAvailable: Boolean = Option(clusterId).isDefined
          if (clusterIdAvailable && !clusterNodes.isEmpty) {
            Result.healthy("OK")
          } else {
            Result.unhealthy("Error connecting to Kafka Cluster")
          }
        }

      try {
        Await.result[Result](healthFuture, checkDuration)
      } catch {
        case e: TimeoutException => Result.unhealthy("Error connecting to Kafka Cluster")
      }

    }.execute()
    Health(health.isHealthy, health.getMessage, Option(health.getError))
  }

  def start(): Future[Http.ServerBinding] = Http().bindAndHandle(route, "0.0.0.0", settings.port)
}

object Api {
  def apply(kafkaClientActorRef: ActorRef)
           (implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) =
    new Api(kafkaClientActorRef)
}

case class ApiSettings(port: Int, timeout: Timeout)

object ApiSettings {
  def apply(config: Config): ApiSettings = ApiSettings(
    config.getInt("api.port"),
    Timeout(Duration.apply(config.getString("api.actor-timeout")).asInstanceOf[FiniteDuration])
  )
}
