import java.util.Properties
import config.KafkaSettings

import scala.concurrent.duration._
import com.typesafe.config.ConfigValueFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, OffsetResetStrategy}
import org.apache.kafka.common.serialization.Deserializer
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestActorRef, TestKit}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import kafka.admin.RemoraKafkaConsumerGroupService
import models.{KafkaClusterHealthResponse, Node}
import io.github.embeddedkafka.Codecs.stringDeserializer
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.CommonClientConfigs
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class ApiSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest with PlayJsonSupport with Eventually {
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
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaHost)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString.toLowerCase)
    props
  }

  val consumer = new SimpleKafkaConsumer[String, String](consumerProps,
    topic,
    implicitly[Deserializer[String]],
    implicitly[Deserializer[String]],
    (record : ConsumerRecords[String, String]) => logger.info(record.toString))

  override def beforeAll(): Unit = {
    EmbeddedKafka.start()
    consumer.start()
  }

  override def afterAll(): Unit = {
    consumer.stop()
    EmbeddedKafka.stop()
    TestKit.shutdownActorSystem(system)
  }

  "GET /consumers" should "return a 200 and list of consumer groups" in {
    eventually {
      Get("/consumers") ~> ApiTest.route ~> check {
        status shouldBe OK
        entityAs[JsArray].value.contains(JsString(consumerGroup)) shouldBe true
      }
    }
  }

  "GET /consumers/consumerGroup" should "return a 200 and consumer group information" in {
    eventually {
      Get(s"/consumers/${consumerGroup}") ~> ApiTest.route ~> check {
        status shouldBe OK
        val response = entityAs[JsValue]
        val partitionAssignmentState = (response \ "partition_assignment").as[JsArray].value
        val lagPerTopic = (response \ "lag_per_topic").as[JsValue]
        (lagPerTopic \ "test-topic").get.asInstanceOf[JsNumber].value shouldBe 0
        partitionAssignmentState.nonEmpty shouldBe true
      }
    }
  }

  "GET" should "return a 200 with cluster JSON info to /health" in {
    Get("/health") ~> ApiTest.route ~> check {
      status shouldBe OK
      contentType shouldBe ContentTypes.`application/json`

      import scala.jdk.CollectionConverters._
      import JsonOps.clusterHealthReads

      val clusterDesc = kafkaSettings.adminClient.describeCluster

      val waitFor = patienceConfig.timeout
      val clusterId = clusterDesc.clusterId.get(waitFor.length, waitFor.unit)
      val controller = clusterDesc.controller.get(waitFor.length, waitFor.unit)
      val nodes = clusterDesc.nodes.get(waitFor.length, waitFor.unit).asScala

      val resp = KafkaClusterHealthResponse(clusterId, Node.from(controller), nodes.map(Node.from).toSeq)
      val entity = Json.fromJson[KafkaClusterHealthResponse](entityAs[JsValue])

      assert(entity.isSuccess)
      entity.get.clusterId shouldBe resp.clusterId
      entity.get.controller shouldBe resp.controller
      entity.get.nodes.length shouldBe 1
      entity.get.nodes shouldBe resp.nodes
    }
  }
}
