package org.kae.quadrantscraper

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    Quadrant.resource[IO].use { q =>
      for
        nonce    <- q.nonce
        username <- IO.print("Username: ") *> IO.readLine
        password <- IO.print("Password: ") *> IO.readLine
        session  <- q.session(username, password, nonce)
        _ <- q
          .pdfsInSite(session)
          .evalTap(IO.println)
          .parEvalMapUnordered(4)(q.downloadPdf(session))
          .compile
          .drain
      yield ExitCode.Success
    }

end Main
