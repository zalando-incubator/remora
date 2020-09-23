package reporter

import com.blacklocus.metrics.CloudWatchReporterBuilder
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class CloudWatchReporterSpec extends FlatSpec with Matchers with PrivateMethodTester with MockFactory {

  private val metricRegistry: MetricRegistry = new MetricRegistry
  private val metric: Metric = mock[Metric]


  "Metrics filter" should "match any metric when empty string filter is given" in {
    val filter = buildMetricFilter(List.empty)

    filter.matches("any_metrics_name", metric) should be(true)
  }

  "Metrics filter" should "match only metrics that are part of the filter string (false: does not contain)" in {
    val filter = buildMetricFilter(List("storage fo", "stofs"))

    filter.matches("stor", metric) should be(false)
  }

   "Metrics filter" should "match only metrics that are part of the filter string (true: contains)" in {
    val filter = buildMetricFilter(List("stor"))

    filter.matches("storage", metric) should be(true)
  }


  "Metrics filter" should "match only metrics that are part of the filter string (false: incorrect)" in {
    val filter = buildMetricFilter(List("storage fo"))

    filter.matches("storagx", metric) should be(false)
  }

  private def buildMetricFilter(filterString: List[String]): MetricFilter = {
    new MetricFilter() {
    //since matches has is a string, the whitelist should be log filenames with commas with spaces
        override def matches(string: String, metric: Metric): Boolean =  {
            if (filterString == List.empty) return true
            for (v <- filterString) {
                if (string.contains(v)) {
                    return true
                }
            }
            return false

        }
    }
  }
}
