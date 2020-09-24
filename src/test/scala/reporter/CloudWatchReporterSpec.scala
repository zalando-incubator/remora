package reporter

import com.blacklocus.metrics.CloudWatchReporterBuilder
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import Utils.buildMetricFilter
import scala.util.matching.Regex


class CloudWatchReporterSpec extends FlatSpec with Matchers with PrivateMethodTester with MockFactory {

  private val metricRegistry: MetricRegistry = new MetricRegistry
  private val metric: Metric = mock[Metric]


  "Metrics filter" should "match any metric when empty string filter is given" in {
    val filter = buildMetricFilter("")

    filter.matches("any_metrics_name", metric) should be(true)
    filter.matches("xfaewojz", metric) should be(true)
  }

  "Metrics filter" should "match metrics that have the regex pattern: true" in {
    val filter = buildMetricFilter("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)")

    filter.matches("gauge.readings_V1-LFS-loader-aws.lag", metric) should be(true)
  }

   "Metrics filter" should "match metrics that have the regex pattern: false" in {
    val filter = buildMetricFilter("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)")

    filter.matches("gauge.vers.stop.lag", metric) should be(false)
  }
}
