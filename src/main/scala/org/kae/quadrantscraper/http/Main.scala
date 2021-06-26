package org.kae.quadrantscraper.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    Quadrant.resource[IO].use { q =>
      for
        nonce    <- q.nonce
        username <- IO.print("Username: ") *> IO.readLine
        password <- IO.print("Password: ") *> IO.readLine
        session  <- q.session(username, password, nonce)
        _        <- IO.println(session)
        _ <- q
          .pdfsInSite(session)
          .evalTap(IO.println)
          .parEvalMapUnordered(4)(q.downloadPdf(session))
          .compile
          .drain
      yield ExitCode.Success
    }

end Main
