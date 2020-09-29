
import com.typesafe.scalalogging.LazyLogging
import com.codahale.metrics.{Metric, MetricFilter}
import scala.util.matching.Regex

object CloudWatchMetricFilter extends LazyLogging {
    def buildMetricFilter(filterRegexPattern: Regex): MetricFilter = {
        new MetricFilter() {
            override def matches(string: String, metric: Metric): Boolean = filterRegexPattern.findFirstIn(string).nonEmpty
        }
    }
}

