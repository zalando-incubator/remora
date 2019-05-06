package reporter

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import config.DataDog
import models.RegistryKafkaMetric
import org.coursera.metrics.datadog.TaggedName.TaggedNameBuilder
import org.coursera.metrics.datadog.transport.UdpTransport
import org.coursera.metrics.datadog.{DatadogReporter, MetricNameFormatter}

class RemoraDatadogReporter(metricRegistry: MetricRegistry, datadogConfig: DataDog) {

  private val transport = new UdpTransport.Builder().withStatsdHost(datadogConfig.agentHost).withPort(datadogConfig.agentPort).build

  /**
    * To avoid sending all metrics for all Kafka consumer groups, this filter allows to restrict which consumer group a Remora instance is interested in.
    */
  private val kafkaConsumerGroupFilter: MetricFilter = new MetricFilter {
    override def matches(metricName: String, metric: Metric): Boolean = {
      val trackedConsumerGroups = datadogConfig.trackedConsumerGroups
      trackedConsumerGroups.isEmpty || trackedConsumerGroups.exists(consumerGroupName => metricName.contains(consumerGroupName))
    }
  }

  private def metricNameFormatter(removeTagsFromMetricName: Boolean): MetricNameFormatter = new MetricNameFormatter {
    override def format(nameWithPrefix: String, path: String*): String = {
      RegistryKafkaMetric.decode(nameWithPrefix.replaceFirst(s"${datadogConfig.name}\\.","")) match {
        case Some(registryKafkaMetric) =>
          val builder = new TaggedNameBuilder().metricName(
            if (removeTagsFromMetricName) buildNameWithoutTags(registryKafkaMetric) else nameWithPrefix
          ).addTag("topic", registryKafkaMetric.topic)
            .addTag("group", registryKafkaMetric.group)
            .addTag("cluster", datadogConfig.clusterTag)
          registryKafkaMetric.partition.foreach(p => builder.addTag("partition", p))
          builder.build().encode()
        case None => nameWithPrefix
      }
    }
  }

  private def buildNameWithoutTags(registryKafkaMetric: RegistryKafkaMetric): String =
    s"${datadogConfig.name}.${registryKafkaMetric.prefix}.${registryKafkaMetric.suffix}"

  def startReporter(): Unit = DatadogReporter
    .forRegistry(metricRegistry)
    .withPrefix(datadogConfig.name)
    .withTransport(transport)
    .filter(kafkaConsumerGroupFilter)
    .withMetricNameFormatter(metricNameFormatter(datadogConfig.removeTagsFromMetricName))
    .build
    .start(datadogConfig.intervalMinutes, TimeUnit.MINUTES)
}
