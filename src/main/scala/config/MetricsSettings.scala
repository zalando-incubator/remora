package config

import com.typesafe.config.Config

case class RegistryOptions(enabled: Boolean = true, intervalSeconds : Int)

case class MetricsSettings(cloudWatchEnabled: Boolean = false, registryOptions: RegistryOptions)

object MetricsSettings {
  def apply(config: Config): MetricsSettings =
    MetricsSettings(config.getBoolean("metrics.cloudwatch"),
      RegistryOptions(config.getBoolean("metrics.toregistry.enabled"),
        config.getInt("metrics.toregistry.interval_seconds")))
}
