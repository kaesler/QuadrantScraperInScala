package org.kae.quadrantscraper.selenium

import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for
      username <- IO.print("Username: ") *> IO.readLine
      password <- IO.print("Password: ") *> IO.readLine

      _ <- Resource
        .both(
          Discoverer.resource[IO](username, password),
          Downloader.resource[IO]
        )
        .use(discoverAndDownloadDocs)
    yield ExitCode.Success

  private def discoverAndDownloadDocs(
    discoverer: Discoverer[IO],
    downloader: Downloader[IO]
  ): IO[Unit] =
    discoverer.allDocsOnSite
      .evalFilter { (docId, _) => DocRepo.docNotAlreadyDownloaded[IO](docId) }
      .parEvalMapUnordered(4)(downloader.downloadDoc.tupled)
      .compile
      .count
      .flatMap { n =>
        summon[Logger[IO]].info(s"$n downloaded")
      }
