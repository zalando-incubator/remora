lazy val commonSettings = Seq(
  name         := "remora",
  organization := "de.zalando",
  scalaVersion := "2.11.8"
)

lazy val dockerSettings = Seq(
  daemonUser in Docker           := "root",
  dockerBaseImage in Docker      := "registry.opensource.zalan.do/stups/openjdk:8u91-b14-1-22",
  dockerExposedPorts in Docker   := Seq(9000),
  dockerExposedVolumes in Docker := Seq("/opt/docker/logs"),
  dockerRepository in Docker     := sys.props.get("docker.repo"),
  maintainer in Docker           := "team-setanta@zalando.ie"
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
    JavaServerAppPackaging
  )

libraryDependencies ++= Seq(
  "ch.qos.logback"            %  "logback-classic"              % "1.1.8",
  "de.heikoseeberger"         %% "akka-http-play-json"          % "1.15.0",
  "nl.grons"                  %% "metrics-scala"                % "3.5.5_a2.3",
  "io.dropwizard.metrics"     %  "metrics-json"                 % "3.1.2",
  "io.dropwizard.metrics"     %  "metrics-jvm"                  % "3.1.2",
  "com.typesafe.akka"         %% "akka-actor"                   % "2.4.16",
  "com.typesafe.akka"         %% "akka-http"                    % "10.0.5",
  "com.typesafe.akka"         %% "akka-http-testkit"            % "10.0.5"  % "test",
  "com.typesafe.akka"         %% "akka-slf4j"                   % "2.4.16",
  "org.apache.httpcomponents" %  "httpcore"                     % "4.4.5",
  "org.apache.httpcomponents" %  "httpclient"                   % "4.5.2",
  "org.scalaz"                %% "scalaz-core"                  % "7.2.8",
  "com.typesafe.play"         %% "play-json"                    % "2.4.8",
  "org.apache.kafka"          %% "kafka"                        % "0.10.2.1",
  "org.scalatest"             %% "scalatest"                    % "2.2.4"   % "test",
  "org.scalamock"             %% "scalamock-scalatest-support"  % "3.2"     % "test",
  "net.manub"                 %% "scalatest-embedded-kafka"     % "0.13.1"  % "test"
)

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
  case default => (assemblyMergeStrategy in assembly).value(default)
}
