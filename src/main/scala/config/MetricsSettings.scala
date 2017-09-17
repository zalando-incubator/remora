package config

import com.typesafe.config.Config

case class RegistryOptions(enabled: Boolean = true, intervalSeconds : Int)
case class CloudWatch(enabled: Boolean = false, name : String, intervalMinutes: Int)

case class MetricsSettings(cloudWatch: CloudWatch, registryOptions: RegistryOptions)

object MetricsSettings {
  def apply(config: Config): MetricsSettings =
    MetricsSettings(
      CloudWatch(config.getBoolean("metrics.cloudwatch.enabled"),
        config.getString("metrics.cloudwatch.name"),
        config.getInt("metrics.cloudwatch.interval_minutes")),
      RegistryOptions(config.getBoolean("metrics.toregistry.enabled"),
        config.getInt("metrics.toregistry.interval_seconds")))
}