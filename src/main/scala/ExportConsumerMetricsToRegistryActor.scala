import akka.actor._
import nl.grons.metrics.scala.{ActorInstrumentedLifeCycle, ReceiveCounterActor, ReceiveExceptionMeterActor, ReceiveTimerActor}

object ExportConsumerMetricsToRegistryActor {
  def props(kafkaClientActorRef: ActorRef) = Props(classOf[ExportConsumerMetricsToRegistryActor], kafkaClientActorRef)
}

class BaseExportConsumerMetricsToRegistryActor(kafkaClientActorRef: ActorRef)
  extends Actor
    with ActorLogging
    with nl.grons.metrics.scala.DefaultInstrumented
    with ActorInstrumentedLifeCycle {
  def receive = {
    case _ => log.info("Tick!")
  }
}

class ExportConsumerMetricsToRegistryActor(kafkaClientActorRef: ActorRef)
  extends BaseExportConsumerMetricsToRegistryActor(kafkaClientActorRef)
    with ReceiveCounterActor
    with ReceiveTimerActor
    with ReceiveExceptionMeterActor
