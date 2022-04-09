import models.RegistryKafkaMetric
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModelsSpec extends AnyFlatSpec with Matchers {

  "RegistryKafkaMetric" should "be encoded as a string as expected with partition" in {
    val metric = RegistryKafkaMetric("gauge","topic", Some("partition"), "group","lag")
    RegistryKafkaMetric.encode(metric) should be("gauge.topic.partition.group.lag")
  }

  it should "be decoded from string without partition" in {
    val stringMetric = "gauge.topic.group.lag"
    RegistryKafkaMetric.decode(stringMetric) should be(Some(RegistryKafkaMetric("gauge","topic", None,"group","lag")))
  }

  it should "be encoded as a string as expected without partition" in {
    val metric = RegistryKafkaMetric("gauge","topic", None, "group","lag")
    RegistryKafkaMetric.encode(metric) should be("gauge.topic.group.lag")
  }

  it should "be decoded from string with partition" in {
    val stringMetric = "gauge.topic.partition.group.lag"
    RegistryKafkaMetric.decode(stringMetric) should be(Some(RegistryKafkaMetric("gauge","topic", Some("partition"),"group","lag")))
  }

  it should "return None if metric name is not standard" in {
    val stringMetric = "gauge.faulty"
    RegistryKafkaMetric.decode(stringMetric) should be(None)
  }
}
