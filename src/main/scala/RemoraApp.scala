import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, MemoryUsageGaugeSet, GarbageCollectorMetricSet}
import config.{MetricsSettings, KafkaSettings}

import scala.util.control.NonFatal
import scala.concurrent.duration._

object RemoraApp extends App with nl.grons.metrics.scala.DefaultInstrumented {

  private val actorSystemName: String = "remora"
  implicit val actorSystem = ActorSystem(actorSystemName)

  metricRegistry.registerAll(new GarbageCollectorMetricSet)
  metricRegistry.registerAll(new MemoryUsageGaugeSet)
  metricRegistry.registerAll(new ThreadStatesGaugeSet)

  lazy val decider: Supervision.Decider = {
    case _: IOException | _: ConnectException | _: TimeoutException => Supervision.Restart
    case NonFatal(err: Throwable) =>
      actorSystem.log.error(err, "Unhandled Exception in Stream: {}", err.getMessage)
      Supervision.Stop
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))(actorSystem)

  implicit val executionContext = actorSystem.dispatchers.lookup("kafka-consumer-dispatcher")
  val kafkaSettings = KafkaSettings(actorSystem.settings.config)
  val consumer = new RemoraKafkaConsumerGroupService(kafkaSettings)
  val kafkaClientActor = actorSystem.actorOf(KafkaClientActor.props(consumer), name = "kafka-client-actor")

  Api(kafkaClientActor).start()

  val metricsSettings = MetricsSettings(actorSystem.settings.config)

  if (metricsSettings.registryOptions.enabled) {
    val exportConsumerMetricsToRegistryActor =
      actorSystem.actorOf(ExportConsumerMetricsToRegistryActor.props(kafkaClientActor),
        name = "export-consumer-metrics-actor")
    actorSystem.scheduler.schedule(0 second, metricsSettings.registryOptions.intervalSeconds second, exportConsumerMetricsToRegistryActor, "export")
  }

}