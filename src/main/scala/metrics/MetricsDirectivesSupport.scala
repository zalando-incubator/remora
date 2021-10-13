package metrics

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directive0, RequestContext, RouteResult}
import akka.http.scaladsl.server.directives.BasicDirectives.{extractRequestContext, mapInnerRoute, mapRouteResult}
import com.codahale.metrics.MetricRegistry
import MetricUtils.{findAndRegisterMeter, findAndRegisterTimer}

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

private[metrics] object MetricsDirectivesSupport {

  def meterByStatus(nameFunc: RequestContext => String)(implicit registry: MetricRegistry): Directive0 = mapInnerRoute { inner =>ctx =>
    inner(ctx).andThen {
      case Success(RouteResult.Complete(resp))  => findAndRegisterMeter(s"${nameFunc(ctx)}-${liftStatusCode(resp.status)}").mark()
      case Success(RouteResult.Rejected(_))     => findAndRegisterMeter(s"${nameFunc(ctx)}-rejections").mark()
      case _: Failure[_]                        => findAndRegisterMeter(s"${nameFunc(ctx)}-failures").mark()
    }(ctx.executionContext)
  }

  private def liftStatusCode(code: StatusCode): String = code match {
    case StatusCodes.Informational(_) => "1xx"
    case StatusCodes.Success(_) => "2xx"
    case StatusCodes.Redirection(_) => "3xx"
    case StatusCodes.ClientError(_) => "4xx"
    case StatusCodes.ServerError(_) => "5xx"
    case StatusCodes.CustomStatusCode(custom) => custom.toString
  }

  def timer(nameFunc: RequestContext => String)(implicit registry: MetricRegistry): Directive0 = extractRequestContext.flatMap { ctx =>
    val timer = findAndRegisterTimer(nameFunc(ctx)).time()
    mapRouteResult { result =>
      timer.stop()
      result
    }
  }

}
