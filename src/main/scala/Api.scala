
import java.util.concurrent.TimeUnit

import KafkaClientActor.{Command, DescribeKafkaClusterConsumer, ListConsumers}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, StandardRoute}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheck.Result
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

import scala.concurrent.duration._
import scala.reflect.ClassTag

class Api(kafkaClientActorRef: ActorRef)
         (implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
  extends nl.grons.metrics.scala.DefaultInstrumented with PlayJsonSupport {

  implicit val apiExecutionContext = actorSystem.dispatchers.lookup("api-dispatcher")

  implicit def remoraExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: RuntimeException =>
        extractUri { uri =>
          actorSystem.log.error(e, "Request received an exception: {}", e.getMessage)
          complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal Error! Please check logs"))
        }
    }

  val settings = ApiSettings(actorSystem.settings.config)

  implicit val duration: Timeout = settings.timeout


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
          healthCheck
        } ~ pathPrefix("consumers") {
          pathEnd {
            complete(askFor[List[String]](ListConsumers).map(Json.toJson(_)))
          } ~ path(Segment) { consumerGroup =>
            complete(askFor[GroupInfo](DescribeKafkaClusterConsumer(consumerGroup)).map(Json.toJson(_)))
          }
        }
      }
    }

  def healthCheck: StandardRoute = {
    getHealth match {
      case Health(true, message, None) => complete(Json.toJson(message).toString())
      case Health(false, _, Some(e)) => failWith(e)
    }
  }

  def getHealth: Health = {
    val health = healthCheck("Health") {
      Result.healthy("OK")
    }.execute()
    Health(health.isHealthy, health.getMessage, Option(health.getError))
  }

  def start() = Http().bindAndHandle(route, "0.0.0.0", settings.port)
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
