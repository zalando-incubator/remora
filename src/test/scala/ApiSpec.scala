import java.util.Properties

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigValueFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import net.manub.embeddedkafka.Codecs.stringDeserializer
import net.manub.embeddedkafka.ConsumerExtensions._
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.json._

import scala.concurrent.duration._

class ApiSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest with PlayJsonSupport with Eventually {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(30, Seconds)), interval = scaled(Span(1000, Millis))) // scalastyle:ignore magic.number

  implicit val config = EmbeddedKafkaConfig()

  val topic = "test-topic"
  val kafkaHost = s"localhost:${config.kafkaPort}"
  val kafkaSettings = KafkaSettings(system.settings.config.withValue("kafka.endpoint", ConfigValueFactory.fromAnyRef(kafkaHost)))
  val consumeGroupService = new RemoraKafkaConsumerGroupService(kafkaSettings)
  val kafkaClientActor = TestActorRef(new KafkaClientActor(consumeGroupService))
  val ApiTest = Api(kafkaClientActor)

  val consumerGroup = "test-consumer-group"

  val consumerProps: Properties = {
    val props = new Properties()
    props.put("group.id", consumerGroup)
    props.put("bootstrap.servers", kafkaHost)
    props.put("auto.offset.reset", "earliest")
    props
  }

  val consumer = new KafkaConsumer[String, String](consumerProps,
    implicitly[Deserializer[String]],
    implicitly[Deserializer[String]])


  override def beforeAll: Unit = {
    EmbeddedKafka.start()
    consumer.consumeLazily(topic)
  }

  override def afterAll {
    consumer.close()
    EmbeddedKafka.stop()
    TestKit.shutdownActorSystem(system)
  }

  "GET /consumers" should "return a 200 and list of consumer groups" in {
    eventually {
      Get("/consumers") ~> ApiTest.route ~> check {
        status should be(OK)
        entityAs[JsArray].value.contains(JsString(consumerGroup)) should be(true)
      }
    }
  }

  "GET /consumers/consumerGroup" should "return a 200 and consumer group information" in {
    eventually {
      Get(s"/consumers/${consumerGroup}") ~> ApiTest.route ~> check {
        status should be(OK)
        val response = entityAs[JsValue]
        val partitionAssignmentState = (response \ "partition_assignment").as[JsArray].value
        partitionAssignmentState.size should be > 0
      }
    }
  }

  "GET" should "return a 200 to /health" in {
    Get("/health") ~> ApiTest.route ~> check {
      status should be(OK)
      entityAs[String] should be("\"OK\"")
    }
  }
}
