package org.kae.quadrantscraper.selenium

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import org.openqa.selenium.By
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.jdk.CollectionConverters.*

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for
      username <- IO.print("Username: ") *> IO.readLine
      password <- IO.print("Password: ") *> IO.readLine
      map      <- Quadrant.resource[IO](username, password).use(_.pdfsByYear)
      set = map.toSet
      _ <- IO.println(set)
    // TODO:
    //    - compute set already downloaded
    //    - compute set difference
    //    - do downloads
    yield ExitCode.Success

// Next:
//  - Downloader: fetches them and stores them
