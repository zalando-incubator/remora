package metrics

import akka.http.scaladsl.server.Directive0
import com.codahale.metrics.MetricRegistry

trait MetricsDirectives {

  import MetricUtils._, MetricsDirectivesSupport._

  protected implicit def metricRegistry: MetricRegistry

  def withMeterByStatusCode: Directive0 = meterByStatus(getMetricName)

  def withTimer: Directive0 = timer(getMetricName)
}
