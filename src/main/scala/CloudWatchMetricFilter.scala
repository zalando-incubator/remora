
import com.typesafe.scalalogging.LazyLogging
import com.codahale.metrics.{Metric, MetricFilter}
import scala.util.matching.Regex

object CloudWatchMetricFilter extends LazyLogging {
    def buildMetricFilter(filterRegexPattern: Regex): MetricFilter = {
        return new MetricFilter() {
            override def matches(string: String, metric: Metric): Boolean =  {
                return filterRegexPattern.findFirstIn(string).nonEmpty
            }
        }
    }
}

