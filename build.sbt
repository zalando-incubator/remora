lazy val commonSettings = Seq(
  name := "remora",
  organization := "de.zalando",
  scalaVersion := "2.11.8"
)

lazy val dockerSettings = Seq(
  daemonUser := "root",
  dockerUpdateLatest := true,
  dockerBaseImage := "registry.opensource.zalan.do/library/openjdk-8:8-20201005",
  dockerExposedPorts := Seq(9000),
  dockerExposedVolumes := Seq("/opt/docker/logs"),
  dockerRepository := sys.props.get("docker.repo"),
  maintainer := "team-setanta@zalando.ie"
)

lazy val gitSettings = Seq(
  git.useGitDescribe := true
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(gitSettings)
  .enablePlugins(
    GitVersioning,
    JavaServerAppPackaging,
    ScmSourcePlugin
  )

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

resolvers ++= Seq("bintray-backline-open-source-releases" at "https://dl.bintray.com/backline/open-source")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.10",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.15.0",
  "nl.grons" %% "metrics-scala" % "3.5.5_a2.3",
  "io.dropwizard.metrics" % "metrics-json" % "3.1.2",
  "io.dropwizard.metrics" % "metrics-jvm" % "3.1.2",
  "com.blacklocus" % "metrics-cloudwatch" % "0.4.0",
  "org.coursera" % "dropwizard-metrics-datadog" % "1.1.13",
  "backline" %% "akka-http-metrics" % "1.0.0",
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % "1.11.189",
  "com.amazonaws" % "aws-java-sdk-sts" % "1.11.775",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
  "org.apache.httpcomponents" % "httpcore" % "4.4.5",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.22",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.22",
  "org.scalaz" %% "scalaz-core" % "7.2.8",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "org.apache.kafka" %% "kafka" % "2.2.1",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "net.manub" %% "scalatest-embedded-kafka" % "2.0.0" % "test"
)

excludeDependencies ++= Seq(
  "org.slf4j" % "slf4j-log4j12",
  "log4j" % "log4j"
)

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
  case default => (assemblyMergeStrategy in assembly).value(default)
}
