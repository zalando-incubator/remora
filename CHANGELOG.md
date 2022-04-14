# v2.0.0

 - Added support for Kafka 3.1.0.
 - Breaking change introduced for metrics exported to AWS CloudWatch.
 - Upgraded Docker base image to use Java 17.
 - Upgraded all dependencies caused by migration from SBT (v0.13 to v1.6), and Scala (v2.11 to v2.13).
 - Moved team name (ownership) to 'Buffalo' in Docker / Pierone. Images are now published to `registry-write.opensource.zalan.do/buffalo` instead of `registry-write.opensource.zalan.do/machina`.

# v1.3.0

 - Added support for filtering CloudWatch metrics.
 - Added support for configuring AdminClient in KafkaSettings.
 - Added support for specifying root log level via Java options.

# v1.2.0

 - Added support for Kafka 2.2.0.
 - Improved DataDog metric name format (reducing redundancy with metric tags).

# v1.1.0

 - Added improved application health checks.

# v1.0.7

 - Added support for Kafka 2.0.0.

# v1.0.6

 - Added support for Kafka 1.1.0.
 - Added support for connecting to Kafka brokers using authentication.

# v1.0.3

 - Moved team name (ownership) to 'Machina' in Docker / Pierone. Images are now published to `registry-write.opensource.zalan.do/machina` instead of `registry-write.opensource.zalan.do/dougal`.

# v1.0.2

 - Added support for reporting aggregated consumer lag in exported metrics.

# v1.0.1

 - Added support for publishing metrics to Datadog.

# v1.0.0

 - Added support for Kafka 1.0.0.

# v0.3.0

 - Added support for exporting lag metrics to AWS CloudWatch.

# v0.2.1

 - Fixed logging issues.

# v0.2.0

 - Support added for Kafka 0.10.2.0.

# v0.1.0

 - Initial release for Kafka 0.10.0.1.
