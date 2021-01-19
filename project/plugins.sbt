addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")
addSbtPlugin("org.scalameta"       % "sbt-mdoc"        % "2.2.16")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.25")
addSbtPlugin("com.github.tkawachi"  % "sbt-doctest"     % "0.9.8")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"      % "0.4.3")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"      % "5.4.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"         % "0.4.0")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.1"
