lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.3.0",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.8.16",
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % "3.8.16",
      "org.typelevel"          %% "cats-effect"     % "3.5.1" withSources (),
      "org.typelevel"          %% "log4cats-slf4j"  % "2.6.0" withSources (),
      "ch.qos.logback"          % "logback-classic" % "1.4.8",
      "org.seleniumhq.selenium" % "selenium-java"   % "4.10.0",
      "org.scalameta"          %% "munit"           % "0.7.29",
      "co.fs2"                 %% "fs2-core"        % "3.7.0"
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
