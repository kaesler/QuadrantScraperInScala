lazy val root = project
  .in(file("."))
  .settings(
    name         := "QuadrantScraper",
    version      := "0.1.0",
    scalaVersion := "3.5.0",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"                 % "3.8.16",
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % "3.9.7",
      "org.typelevel"                 %% "cats-effect"          % "3.5.4" withSources (),
      "org.typelevel"                 %% "log4cats-slf4j"       % "2.7.0" withSources (),
      "ch.qos.logback"                 % "logback-classic"      % "1.5.6",
      "org.seleniumhq.selenium"        % "selenium-java"        % "4.23.0",
      "org.scalameta"                 %% "munit"                % "1.0.0",
      "co.fs2"                        %% "fs2-core"             % "3.10.2"
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
