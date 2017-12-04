package reporter


import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import config.DataDog
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class RemoraDatadogReporterSpec extends FlatSpec with Matchers with PrivateMethodTester with MockFactory {

  private val metricRegistry: MetricRegistry = new MetricRegistry
  private val metric: Metric = mock[Metric]

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


  private def buildMetricFilter(kafkaConsumerList: List[String]): MetricFilter = {
    val config = DataDog(enabled = true, "test", 1, "localhost", 8125, kafkaConsumerList)
    val reporter = new RemoraDatadogReporter(metricRegistry, config)
    reporter invokePrivate PrivateMethod[MetricFilter]('kafkaConsumerGroupFilter)()
  }
}
