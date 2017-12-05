resolvers += Resolver.url("sbt-plugins", url("https://dl.bintray.com/zalando/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n"       % "sbt-assembly"        % "0.14.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"             % "0.8.5")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager" % "1.1.5")
addSbtPlugin("ie.zalando.buffalo" % "sbt-scm-source"    % "0.0.5")