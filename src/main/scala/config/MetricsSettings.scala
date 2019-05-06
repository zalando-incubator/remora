package config

import com.typesafe.config.Config
import scala.collection.JavaConverters._

case class RegistryOptions(enabled: Boolean = true, intervalSeconds: Int)
case class CloudWatch(enabled: Boolean = false, name: String, intervalMinutes: Int)

case class DataDog(enabled: Boolean = false,
                   name: String,
                   intervalMinutes: Int,
                   agentHost: String,
                   agentPort: Int,
                   trackedConsumerGroups: List[String],
                   removeTagsFromMetricName: Boolean,
                   clusterTag: String)

case class MetricsSettings(cloudWatch: CloudWatch, dataDog: DataDog, registryOptions: RegistryOptions)

object MetricsSettings {
  def apply(config: Config): MetricsSettings =
    MetricsSettings(
      CloudWatch(
        config.getBoolean("metrics.cloudwatch.enabled"),
        config.getString("metrics.cloudwatch.name"),
        config.getInt("metrics.cloudwatch.interval_minutes")
      ),
      DataDog(
        config.getBoolean("metrics.datadog.enabled"),
        config.getString("metrics.datadog.name"),
        config.getInt("metrics.datadog.interval_minutes"),
        config.getString("metrics.datadog.host"),
        config.getInt("metrics.datadog.port"),
        config.getStringList("metrics.datadog.tracked_consumer_group").asScala.toList,
        config.getBoolean("metrics.datadog.remove_tags_from_metric_name"),
        config.getString("metrics.datadog.cluster_tag")
      ),
      RegistryOptions(
        config.getBoolean("metrics.toregistry.enabled"),
        config.getInt("metrics.toregistry.interval_seconds")
      )
    )
}
