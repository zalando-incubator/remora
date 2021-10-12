resolvers += Resolver.url("sbt-plugins", url("https://dl.bintray.com/zalando/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n"       % "sbt-assembly"        % "0.14.8")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"             % "1.0.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager" % "1.3.6")
