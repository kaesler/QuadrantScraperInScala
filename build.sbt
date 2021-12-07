lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.1.0",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.3.18",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" %
        "3.3.18",
      "org.typelevel"          %% "log4cats-slf4j"  % "2.1.1" withSources (),
      "ch.qos.logback"          % "logback-classic" % "1.2.7",
      "org.seleniumhq.selenium" % "selenium-java"   % "4.1.0",
      "org.scalameta"          %% "munit"           % "0.7.29",
      "co.fs2"                 %% "fs2-core"        % "3.2.3"
    ),
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings",
      "-unchecked",
      "-language:implicitConversions",
      "-source:future"
    )
  )
