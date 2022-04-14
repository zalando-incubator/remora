package config

import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import com.typesafe.scalalogging.LazyLogging

import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import scala.util.control.NonFatal

class GuardianSupervisorStrategy extends akka.actor.SupervisorStrategyConfigurator with LazyLogging {
  private val decider: SupervisorStrategy.Decider = {
    case _: IOException | _: ConnectException | _: TimeoutException => SupervisorStrategy.Restart
    case NonFatal(err: Throwable) =>
      logger.error( "Unhandled Exception in Stream: {}", err.getMessage)
      SupervisorStrategy.Stop
  }
  override def create(): SupervisorStrategy = {
    OneForOneStrategy()(decider)
  }
}
