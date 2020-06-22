package config

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClientConfig
import org.scalatest.{FlatSpec, Matchers}

class KafkaSettingsSpec extends FlatSpec with Matchers {
  "KafkaSettingsSpec" should "correctly inject the correct endpoint in AdminClient properties" in {
    val conf = ConfigFactory.load("kafka-without-admin-client.conf")
    val settings = KafkaSettings(conf)

    settings.address should ===("localhost:9092")
    settings.commandConfig should ===("test.properties")
    settings.adminClientProps.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG) should ===("localhost:9092")
  }

  it should "correctly parse the correct security protocol if provided in AdminClient properties" in {
    val conf = ConfigFactory.load("kafka-without-admin-client.conf")
    val settings = KafkaSettings(conf)
    val adminClientConfig = new AdminClientConfig(settings.adminClientProps)
    adminClientConfig.getString(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG) should ===(CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL)

    val conf2 = ConfigFactory.load("kafka-with-admin-client.conf")
    val settings2 = KafkaSettings(conf2)
    val adminClientConfig2 = new AdminClientConfig(settings2.adminClientProps)
    adminClientConfig2.getString(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG) should ===("SSL")
  }
}
