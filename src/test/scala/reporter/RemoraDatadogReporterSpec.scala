package reporter


import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import config.DataDog
import org.coursera.metrics.datadog.MetricNameFormatter
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class RemoraDatadogReporterSpec extends FlatSpec with Matchers with PrivateMethodTester with MockFactory {

  private val metricRegistry: MetricRegistry = new MetricRegistry
  private val metric: Metric = mock[Metric]
  private val config = DataDog(enabled = true, "test", 1, "localhost", 8125, List.empty, removeTagsFromMetricName = false, "defaultcluster")
  private val configRemoveTags = DataDog(enabled = true, "test", 1, "localhost", 8125, List.empty, removeTagsFromMetricName = true, "defaultcluster")

  "Metrics filter" should "match any metric when no filter is given" in {
    val filter = buildMetricFilter(List.empty)

    filter.matches("any_metrics_name", metric) should be(true)
  }

  it should "match metric containing consumer group name" in {
    val kafkaConsumerGroupName = "test-consumer1"
    val filter = buildMetricFilter(List(kafkaConsumerGroupName))

    filter.matches(s"metric-name-$kafkaConsumerGroupName", metric) should be(true)
  }

  it should "not match metric containing consumer group name" in {
    val filter = buildMetricFilter(List("test-consumer1"))

    filter.matches("some-metrics", metric) should be(false)
  }

  "Metric name formatter" should "add tag information if metric is well formatted" in {
    val formatter = getMetricNameFormatter(config)

    formatter.format(s"${config.name}.gauge.test.1.test-consumer.lag") should be(s"${config.name}.gauge.test.1.test-consumer.lag[topic:test,group:test-consumer,partition:1]")
  }

  it should "not add partition tag information if no partition" in {
    val formatter = getMetricNameFormatter(config)

    formatter.format(s"${config.name}.gauge.test-topic.test-consumer.totalLag") should be(s"${config.name}.gauge.test-topic.test-consumer.totalLag[topic:test-topic,group:test-consumer]")
  }

  it should "not add tag information otherwise" in {
    val formatter = getMetricNameFormatter(config)

    formatter.format(s"${config.name}.gauge.test_1_faulty_test-consumer__lag") should be(s"${config.name}.gauge.test_1_faulty_test-consumer__lag")
  }

  "Metric name formatter without tags" should "add tag information if metric is well formatted" in {
    val formatter = getMetricNameFormatter(configRemoveTags)

    formatter.format(s"${configRemoveTags.name}.gauge.test.1.test-consumer.lag") should be(s"${configRemoveTags.name}.gauge.lag[topic:test,group:test-consumer,partition:1,cluster:defaultcluster]")
  }

  it should "not add partition tag information if no partition" in {
    val formatter = getMetricNameFormatter(configRemoveTags)

    formatter.format(s"${configRemoveTags.name}.gauge.test-topic.test-consumer.totalLag") should be(s"${configRemoveTags.name}.gauge.totalLag[topic:test-topic,group:test-consumer,cluster:defaultcluster]")
  }

  private def buildMetricFilter(kafkaConsumerList: List[String], removeTags: Boolean = false): MetricFilter = {
    val config = DataDog(enabled = true, "test", 1, "localhost", 8125, kafkaConsumerList, removeTags)
    val reporter = new RemoraDatadogReporter(metricRegistry, config)
    reporter invokePrivate PrivateMethod[MetricFilter]('kafkaConsumerGroupFilter)()
  }

  private def getMetricNameFormatter(config: DataDog): MetricNameFormatter = {
    val reporter = new RemoraDatadogReporter(metricRegistry, config)
    reporter invokePrivate PrivateMethod[MetricNameFormatter]('metricNameFormatter)(config.removeTagsFromMetricName)
  }
}
