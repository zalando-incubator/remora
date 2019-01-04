package config

import scala.collection.Map
import scala.collection.JavaConverters._
import com.typesafe.config.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient

case class KafkaSettings(address: String, commandConfig: String) {
  lazy val adminClient: AdminClient = AdminClient.create(Map[String, AnyRef](
    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> address
  ).asJava)
}

object KafkaSettings {
  def apply(config: Config): KafkaSettings =
    KafkaSettings(
      config.getString("kafka.endpoint"),
      config.getString("kafka.command.config")
    )
}