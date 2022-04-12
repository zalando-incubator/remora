package reporter

import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import com.blacklocus.metrics.CloudWatchReporterBuilder
import com.codahale.metrics.MetricRegistry
import config.CloudWatch
import filter.CloudWatchMetricFilter

import java.util.concurrent.TimeUnit

class RemoraCloudWatchReporter(metricRegistry: MetricRegistry, cloudWatchConfig: CloudWatch) {
  val amazonCloudWatchAsync: AmazonCloudWatchAsync = AmazonCloudWatchAsyncClientBuilder.defaultClient

  val logMetricFilter = new CloudWatchMetricFilter(cloudWatchConfig.metricFilter)

  def startReporter(): Unit = new CloudWatchReporterBuilder()
    .withNamespace(cloudWatchConfig.name)
    .withRegistry(metricRegistry)
    .withClient(amazonCloudWatchAsync)
    .withFilter(logMetricFilter)
    .build()
    .start(cloudWatchConfig.intervalMinutes, TimeUnit.MINUTES)
}
