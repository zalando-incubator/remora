
import com.typesafe.scalalogging.LazyLogging
import com.codahale.metrics.{Metric, MetricFilter}
import scala.util.matching.Regex

object Utils extends LazyLogging {
    def buildMetricFilter(filterRegexPattern: String): MetricFilter = {
        //since matches has is a string, the whitelist should be log filenames with commas with spaces
        return new MetricFilter() {
            override def matches(string: String, metric: Metric): Boolean =  {
                logger.debug("Metric Name:" + string)
                if (filterRegexPattern.length == 0 ||
                    (new Regex(filterRegexPattern).findFirstIn(string).nonEmpty)) return true
                return false
            }
        }
    }
}

