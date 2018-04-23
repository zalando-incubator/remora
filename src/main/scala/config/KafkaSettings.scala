package config

import com.typesafe.config.Config

case class KafkaSettings(address: String, commandConfig: String)

object KafkaSettings {
  def apply(config: Config): KafkaSettings =
    KafkaSettings(
      config.getString("kafka.endpoint"),
      config.getString("kafka.command.config")
    )
}