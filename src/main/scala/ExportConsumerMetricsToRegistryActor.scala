import KafkaClientActor.{Command, DescribeKafkaClusterConsumer, ListConsumers}
import akka.actor._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.codahale.metrics.Gauge
import models.RegistryKafkaMetric._
import models.{GroupInfo, RegistryKafkaMetric}
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
      consumerList.map(_.foreach(consumerGroup => {
        val groupInfo = askFor[GroupInfo](DescribeKafkaClusterConsumer(consumerGroup))
        groupInfo.map { gi =>
          gi.partitionAssignmentStates.map(pa => {
            pa.map { p =>
              val offsetKey = encode(RegistryKafkaMetric("gauge", p.topic.get, p.partition.map(_.toString), p.group, "offset"))
              registerOrUpdateGauge(offsetKey, p.offset)

              val lagKey = encode(RegistryKafkaMetric("gauge", p.topic.get, p.partition.map(_.toString), p.group, "lag"))
              registerOrUpdateGauge(lagKey, p.lag)

              val logEndKey =  encode(RegistryKafkaMetric("gauge", p.topic.get, p.partition.map(_.toString), p.group, "logend"))
              registerOrUpdateGauge(logEndKey, p.logEndOffset)
            }
            gi.lagPerTopic.map { lagPerTopic =>
              lagPerTopic.foreach { case (topic, totalLag) =>
                val lagKey = encode(RegistryKafkaMetric("gauge", topic, None, consumerGroup, "lag" ))
                registerOrUpdateGauge(lagKey, Some(totalLag))
              }
            }
          }
          )
        }
      }))
  }

  //yea the gauges aren't really meant to be used by this, but i dont want to cache the results.
  def registerOrUpdateGauge(gaugeName: String, value: Option[Long]) = {
    value match {
      case Some(v) => {
        val gauge = metricRegistry.getMetrics.getOrDefault(gaugeName, new Gauge[Long] {
          override def getValue: Long = v
        })
        metricRegistry.remove(gaugeName)
        metricRegistry.register(gaugeName, gauge)
      }
      case None => log.error(s"Gauge $gaugeName has None!")
    }
  }
}

class ExportConsumerMetricsToRegistryActor(kafkaClientActorRef: ActorRef)
                                          (implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
  extends BaseExportConsumerMetricsToRegistryActor(kafkaClientActorRef)
    with ReceiveCounterActor
    with ReceiveTimerActor
    with ReceiveExceptionMeterActor
