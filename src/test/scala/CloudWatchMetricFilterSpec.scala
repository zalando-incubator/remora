
import com.blacklocus.metrics.CloudWatchReporterBuilder
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import CloudWatchMetricFilter.buildMetricFilter
import scala.util.matching.Regex
import java.util.regex.PatternSyntaxException


class CloudWatchMetricFilterSpec extends FlatSpec with Matchers with PrivateMethodTester with MockFactory {

  private val metricRegistry: MetricRegistry = new MetricRegistry
  private val metric: Metric = mock[Metric]


  "buildMetricsFilter" should "match any metric when empty string filter is given" in {
    val filter = buildMetricFilter(new Regex(""))

    filter.matches("any_metrics_name", metric) should be(true)
    filter.matches("xfaewojz", metric) should be(true)
  }

   "it" should "match metrics that have the regex pattern: true" in {
    val filter = buildMetricFilter(new Regex("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)"))

    filter.matches("gauge.readings_V1-LFS-loader-aws.lag", metric) should be(true)
  }

   "it" should "match metrics that have the regex pattern: false" in {
    val filter = buildMetricFilter(new Regex("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)"))

    filter.matches("gauge.vers.stop.lag", metric) should be(false)
  }

   "it" should "error when theres an invalid regex pattern created" in {
    an [PatternSyntaxException] should be thrownBy new Regex("[")
   }
}
