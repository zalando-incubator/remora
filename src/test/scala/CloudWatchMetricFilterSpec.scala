import com.codahale.metrics.Metric
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import filter.CloudWatchMetricFilter

class CloudWatchMetricFilterSpec extends AnyFlatSpec with Matchers with MockFactory {

  private val metric: Metric = mock[Metric]

  "buildMetricsFilter" should "match any metric when empty string filter is given" in {
    val filter = new CloudWatchMetricFilter("".r)

    filter.matches("any_metrics_name", metric) shouldBe true
    filter.matches("xfaewojz", metric) shouldBe true
  }

   "it" should "match metrics that have the regex pattern: true" in {
    val filter = new CloudWatchMetricFilter("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)".r)

    filter.matches("gauge.readings_V1-LFS-loader-aws.lag", metric) shouldBe true
  }

   "it" should "match metrics that have the regex pattern: false" in {
    val filter = new CloudWatchMetricFilter("([a-zA-Z]+.[a-zA-Z].*LFS+-loader-.+.lag)".r)

    filter.matches("gauge.vers.stop.lag", metric) shouldBe false
  }
}
