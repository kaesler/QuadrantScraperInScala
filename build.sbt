lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.2.0",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.7.4",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" %
        "3.7.4",
      "org.typelevel"          %% "cats-effect"     % "3.3.12" withSources (),
      "org.typelevel"          %% "log4cats-slf4j"  % "2.4.0" withSources (),
      "ch.qos.logback"          % "logback-classic" % "1.2.11",
      "org.seleniumhq.selenium" % "selenium-java"   % "4.4.0",
      "org.scalameta"          %% "munit"           % "0.7.29",
      "co.fs2"                 %% "fs2-core"        % "3.2.12"
    ),
    scalacOptions := Seq(
      "-deprecation",
      "-explain-types",
      "-feature",
      "-language:implicitConversions",
      "-source:future",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xmigration"
    )
  )
