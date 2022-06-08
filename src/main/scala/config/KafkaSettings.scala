package config

import com.typesafe.config.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient

import scala.jdk.CollectionConverters._

case class KafkaSettings(address: String, commandConfig: String, adminClientProps: java.util.Properties) {
  lazy val adminClient: AdminClient = AdminClient.create(adminClientProps)
}

object KafkaSettings {
  def apply(config: Config): KafkaSettings = {
    val properties = new java.util.Properties()
    val adminClientConfig = config.getConfig("kafka.admin-client")
      .withValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, config.getValue("kafka.endpoint"))

    org.apache.kafka.clients.admin.AdminClientConfig.configNames().asScala.foreach {
      k => if (adminClientConfig.hasPath(k)) properties.put(k, adminClientConfig.getAnyRef(k))
    }

    KafkaSettings(
      config.getString("kafka.endpoint"),
      config.getString("kafka.command.config"),
      properties
    )
  }
}
