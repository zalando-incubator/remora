import java.util.Properties

import config.KafkaSettings

import scala.concurrent.duration._
import com.typesafe.config.ConfigValueFactory
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.serialization.Deserializer
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestActorRef, TestKit}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import kafka.admin.RemoraKafkaConsumerGroupService
import models.{KafkaClusterHealthResponse, Node}
import net.manub.embeddedkafka.Codecs.stringDeserializer
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import play.api.libs.json._

class ApiSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest with PlayJsonSupport with Eventually {
  private val logger = LoggerFactory.getLogger(ApiSpec.this.getClass.getName)

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(300, Seconds)), interval = scaled(Span(10000, Millis))) // scalastyle:ignore magic.number

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

  val consumer = new SimpleKafkaConsumer[String, String](consumerProps,
    topic,
    implicitly[Deserializer[String]],
    implicitly[Deserializer[String]],
    (record : ConsumerRecords[String, String]) => logger.info(record.toString))

  override def beforeAll: Unit = {
    EmbeddedKafka.start()
    consumer.start()
  }

  override def afterAll {
    consumer.stop()
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
        val lagPerTopic = (response \ "lag_per_topic").as[JsValue]
        (lagPerTopic \ "test-topic").get.asInstanceOf[JsNumber].value should be(0)
        partitionAssignmentState.size should be > 0
      }
    }
  }

  "GET" should "return a 200 with cluster JSON info to /health" in {
    Get("/health") ~> ApiTest.route ~> check {
      status should be(OK)
      contentType should be(ContentTypes.`application/json`)

      import scala.collection.JavaConverters._
      import JsonOps.clusterHealthWrites

      val clusterDesc = kafkaSettings.adminClient.describeCluster

      val waitFor = patienceConfig.timeout
      val clusterId = clusterDesc.clusterId.get(waitFor.length, waitFor.unit)
      val controller = clusterDesc.controller.get(waitFor.length, waitFor.unit)
      val nodes = clusterDesc.nodes.get(waitFor.length, waitFor.unit).asScala

      val resp = KafkaClusterHealthResponse(clusterId, Node.from(controller), nodes.map(Node.from).toSeq)
      entityAs[JsValue] should be(Json.toJson(resp))
    }
  }
}
