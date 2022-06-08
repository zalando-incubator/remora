import akka.actor.ActorSystem
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import com.typesafe.scalalogging.LazyLogging
import config.{KafkaSettings, MetricsSettings}
import kafka.admin.RemoraKafkaConsumerGroupService
import reporter.{RemoraCloudWatchReporter, RemoraDatadogReporter}

import scala.concurrent.duration._

object RemoraApp extends App with nl.grons.metrics4.scala.DefaultInstrumented with LazyLogging {

  private val actorSystemName: String = "remora"
  implicit val actorSystem = ActorSystem(actorSystemName)

  metricRegistry.registerAll(new GarbageCollectorMetricSet)
  metricRegistry.registerAll(new MemoryUsageGaugeSet)
  metricRegistry.registerAll(new ThreadStatesGaugeSet)

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
    actorSystem.scheduler.scheduleAtFixedRate(0.seconds, metricsSettings.registryOptions.intervalSeconds.seconds, exportConsumerMetricsToRegistryActor, "export")
  }

  if (metricsSettings.cloudWatch.enabled) {
    logger.info("Reporting metricsRegistry to Cloudwatch")
    val cloudWatchReporter = new RemoraCloudWatchReporter(metricRegistry, metricsSettings.cloudWatch)
    cloudWatchReporter.startReporter()
  }

  if (metricsSettings.dataDog.enabled) {
    logger.info(s"Reporting metricsRegistry to Datadog at ${metricsSettings.dataDog.agentHost}:${metricsSettings.dataDog.agentPort}")
    val datadogReporter = new RemoraDatadogReporter(metricRegistry, metricsSettings.dataDog)
    datadogReporter.startReporter()
  }
}
