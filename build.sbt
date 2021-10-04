lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.0.2",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.3.15",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" %
        "3.3.15",
      "org.typelevel"          %% "log4cats-slf4j"  % "2.1.1" withSources (),
      "ch.qos.logback"          % "logback-classic" % "1.2.6",
      "org.seleniumhq.selenium" % "selenium-java"   % "3.141.59",
      "org.scalameta"          %% "munit"           % "0.7.29",
      "co.fs2"                 %% "fs2-core"        % "3.1.3"
    )
  )
