val scala3Version = "3.0.0-RC2"
val scala2Version = "2.13.5"

lazy val root = project
  .in(file("."))
  .settings(
    name := "QuadrantScraper",
    version := "0.1.0",
    scalaVersion := scala2Version,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"                           % "3.3.0-RC1",
      "co.fs2"                        %% "fs2-core"                       % "3.0.0",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.0-RC1",
      "org.jsoup"                      % "jsoup"                          % "1.13.1"
    )
  )
