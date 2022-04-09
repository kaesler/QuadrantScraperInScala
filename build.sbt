lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.1.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.5.1",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" %
        "3.5.1",
      "org.typelevel"          %% "cats-effect"     % "3.3.7" withSources (),
      "org.typelevel"          %% "log4cats-slf4j"  % "2.2.0" withSources (),
      "ch.qos.logback"          % "logback-classic" % "1.2.11",
      "org.seleniumhq.selenium" % "selenium-java"   % "4.1.3",
      "org.scalameta"          %% "munit"           % "0.7.29",
      "co.fs2"                 %% "fs2-core"        % "3.2.7",
      "org.typelevel"          %% "cats-effect"     % "3.3.11"
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
