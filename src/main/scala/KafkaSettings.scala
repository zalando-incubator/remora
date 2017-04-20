import com.typesafe.config.Config

case class KafkaSettings(address: String)

object KafkaSettings {
  def apply(config: Config): KafkaSettings =
    KafkaSettings(config.getString("kafka.endpoint"))
}