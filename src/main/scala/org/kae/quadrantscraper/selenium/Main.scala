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

      docsOnSite        <- Discoverer.resource[IO](username, password).use(_.docUris)
      _                 <- IO.println("On site")
      _                 <- IO.println(docsOnSite.mkString("\n"))
      alreadyDownloaded <- DocRepo.create[IO].contents

      toBeDownloaded = docsOnSite -- alreadyDownloaded
      _ <-
        if (toBeDownloaded.isEmpty) then IO.println("No new content")
        else
          IO.println(s"${toBeDownloaded.size} to be downloaded:") *>
            Downloader.resource[IO].use(_.downloadDocs(toBeDownloaded.toList.sortBy(_._1)))
    yield ExitCode.Success
