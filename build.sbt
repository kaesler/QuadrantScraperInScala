lazy val root = project
  .in(file("."))
  .settings(
    name := "QuadrantScraper",
    version := "0.1.0",
    scalaVersion := "3.0.0",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"     % "3.3.7",
      "co.fs2"                        %% "fs2-core" % "3.0.4",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" %
        "3.3.7",
      "org.jsoup"      % "jsoup"           % "1.13.1",
      "org.typelevel" %% "log4cats-slf4j"  % "2.1.1" withSources (),
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
