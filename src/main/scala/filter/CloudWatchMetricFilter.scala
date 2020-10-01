package filter

import com.typesafe.scalalogging.LazyLogging
import com.codahale.metrics.{Metric, MetricFilter}
import scala.util.matching.Regex

class CloudWatchMetricFilter(filterRegexPattern: Regex) extends MetricFilter {
  override def matches(string: String, metric: Metric): Boolean =  {
    filterRegexPattern.findFirstIn(string).nonEmpty
  }
}

