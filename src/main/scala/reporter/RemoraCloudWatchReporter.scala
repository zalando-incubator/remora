package reporter

import com.codahale.metrics.MetricRegistry
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsync, AmazonCloudWatchAsyncClientBuilder}
import config.CloudWatch
import io.github.azagniotov.metrics.reporter.cloudwatch.CloudWatchReporter
import filter.CloudWatchMetricFilter
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

import java.util.concurrent.TimeUnit

class RemoraCloudWatchReporter(metricRegistry: MetricRegistry, cloudWatchConfig: CloudWatch) {
  val amazonCloudWatchAsync: CloudWatchAsyncClient = CloudWatchAsyncClient.create()
  val logMetricFilter = new CloudWatchMetricFilter(cloudWatchConfig.metricFilter)

  def startReporter(): Unit = CloudWatchReporter
    .forRegistry(metricRegistry, amazonCloudWatchAsync, cloudWatchConfig.name)
    .filter(logMetricFilter)
    .build()
    .start(cloudWatchConfig.intervalMinutes, TimeUnit.MINUTES)
}
