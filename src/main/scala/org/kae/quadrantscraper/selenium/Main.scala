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
      username          <- IO.print("Username: ") *> IO.readLine
      password          <- IO.print("Password: ") *> IO.readLine
      docsOnSite        <- Quadrant.resource[IO](username, password).use(_.docUris)
      _                 <- IO.println("On site")
      _                 <- IO.println(docsOnSite.mkString("\n"))
      docsAlreadyCopied <- DocRepo.create[IO].contents
      _                 <- IO.println("Already copied")
      _                 <- IO.println(docsAlreadyCopied.mkString("\n"))
      toBeDownloaded = docsOnSite -- docsAlreadyCopied
      _ <- IO.println("To be downloaded")
      _ <- IO.println(toBeDownloaded.mkString("\n"))
    yield ExitCode.Success

// Next:
//  - Downloader: fetches them and stores them
