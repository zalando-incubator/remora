package reporter

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import config.DataDog
import org.coursera.metrics.datadog.DatadogReporter
import org.coursera.metrics.datadog.transport.UdpTransport

/**
  *
  * @param metricRegistry The metrics registry containing the metrics
  * @param datadogConfig  The configuration for this reporter
  */
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

  def startReporter(): Unit = DatadogReporter
    .forRegistry(metricRegistry)
    .withPrefix(datadogConfig.name)
    .withTransport(transport)
    .filter(kafkaConsumerGroupFilter)
    .build
    .start(datadogConfig.intervalMinutes, TimeUnit.MINUTES)


}

