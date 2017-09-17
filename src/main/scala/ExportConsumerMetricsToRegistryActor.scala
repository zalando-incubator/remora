import KafkaClientActor.{Command, ListConsumers}
import akka.actor._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import nl.grons.metrics.scala.{ActorInstrumentedLifeCycle, ReceiveCounterActor, ReceiveExceptionMeterActor, ReceiveTimerActor}
import scala.concurrent.duration._
import scala.reflect.ClassTag

object ExportConsumerMetricsToRegistryActor {
  def props(kafkaClientActorRef: ActorRef)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) =
    Props(classOf[ExportConsumerMetricsToRegistryActor], kafkaClientActorRef, actorSystem, materializer)
}

class BaseExportConsumerMetricsToRegistryActor(kafkaClientActorRef: ActorRef)
                                              (implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
  extends Actor
    with ActorLogging
    with nl.grons.metrics.scala.DefaultInstrumented
    with ActorInstrumentedLifeCycle {

  implicit val timeout = Timeout(60 seconds)
  implicit val apiExecutionContext = actorSystem.dispatchers.lookup("exporter-dispatcher")

  private def askFor[RES](command: Command)(implicit tag: ClassTag[RES]) =
    (kafkaClientActorRef ? command).mapTo[RES]

  def receive = {
    case _ =>
      log.info("Exporting lag info to metrics registry!")
      val consumerList = askFor[List[String]](ListConsumers)
      consumerList.map(_.foreach(i =>



        log.info(s"Received $i")))
  }
}

class ExportConsumerMetricsToRegistryActor(kafkaClientActorRef: ActorRef)
                                          (implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
  extends BaseExportConsumerMetricsToRegistryActor(kafkaClientActorRef)
    with ReceiveCounterActor
    with ReceiveTimerActor
    with ReceiveExceptionMeterActor
