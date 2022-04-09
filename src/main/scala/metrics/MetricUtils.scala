package metrics


import akka.http.scaladsl.server.RequestContext
import com.codahale.metrics._

import scala.jdk.CollectionConverters._

object MetricUtils {
  def getMetricName(ctx: RequestContext): String = {
    val methodName = ctx.request.method.name
    val routeName = ctx.request.uri.path.toString.drop(1).replaceAll("/", ".")
    s"$routeName.$methodName"
  }

  def findAndRegisterMeter(name: String)(implicit metricRegistry: MetricRegistry): Meter = {
    val found = metricRegistry.getMeters(new NameBasedMetricFilter(name)).values.asScala.headOption
    findAndRegisterMetric(name, new Meter(), found)
  }

  def findAndRegisterTimer(name: String)(implicit metricRegistry: MetricRegistry): Timer = {
    val found = metricRegistry.getTimers(new NameBasedMetricFilter(name)).values.asScala.headOption
    findAndRegisterMetric(name, new Timer(), found)
  }

  protected final class NameBasedMetricFilter(needle: String) extends MetricFilter {
    override def matches(name: String, metric: Metric): Boolean =
      name equalsIgnoreCase needle
  }

  private[this] def findAndRegisterMetric[T <: Metric](
                                                        name: String,
                                                        metric: => T,
                                                        found: Option[T])(implicit metricRegistry: MetricRegistry): T = {
    val m = metric
    try {
      found.getOrElse(metricRegistry.register(name, m))
    }catch {
      case err: IllegalArgumentException if err.getMessage.contains("A metric named") => m
    }
  }
}