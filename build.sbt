val scala3Version = "3.0.0-RC2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ScraperInS3",
    version := "0.1.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.3.0-RC1",
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )
