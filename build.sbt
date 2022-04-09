lazy val commonSettings = Seq(
  name := "remora",
  organization := "de.zalando",
  scalaVersion := "2.13.8"
)

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

lazy val dockerSettings = Seq(
  daemonUser := "root",
  dockerUpdateLatest := false,
  dockerBaseImage := "registry.opensource.zalan.do/library/eclipse-temurin-17-jdk:latest",
  dockerExposedPorts := Seq(9000),
  dockerExposedVolumes := Seq("/opt/docker/logs"),
  dockerRepository := sys.props.get("docker.repo"),
  maintainer := "team-buffalo@zalando.ie"
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(dockerSettings)
<<<<<<< HEAD
  .enablePlugins(JavaServerAppPackaging)
=======
  .settings(gitSettings)
  .enablePlugins(
    JavaServerAppPackaging
  )
>>>>>>> a5599a5 (Update Docker publishing strategy)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.39.2",
  "nl.grons" %% "metrics4-akka_a26" % "4.2.8",
  "io.dropwizard.metrics" % "metrics-json" % "4.2.9",
  "io.dropwizard.metrics" % "metrics-jvm" % "4.2.9",
  "io.github.azagniotov" % "dropwizard-metrics-cloudwatch" % "2.0.8",
  "org.coursera" % "dropwizard-metrics-datadog" % "1.1.14",
  "software.amazon.awssdk" % "aws-sdk-java" % "2.17.166",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "com.typesafe.akka" %% "akka-actor" % "2.6.19",
  "com.typesafe.akka" %% "akka-http" % "10.2.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.19",
  "com.typesafe.akka" %% "akka-stream" % "2.6.19",
  "org.apache.httpcomponents" % "httpcore" % "4.4.15",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "org.joda" % "joda-convert" % "2.2.2",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.36",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.36",
  "org.scalaz" %% "scalaz-core" % "7.3.6",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.apache.kafka" %% "kafka" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.19" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.2.9" % Test,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "io.github.embeddedkafka" %% "embedded-kafka" % "3.1.0" % Test
)

excludeDependencies ++= Seq(
  "org.slf4j" % "slf4j-log4j12",
  "log4j" % "log4j"
)

assembly / assemblyMergeStrategy := {
  case PathList("org", "apache", "commons", "logging", _@_*) => MergeStrategy.first
  case default => (assembly / assemblyMergeStrategy).value(default)
}
