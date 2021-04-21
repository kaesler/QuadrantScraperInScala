package org.kae.quadrantscraper

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      q = Quadrant.create(backend)
      nonce    <- q.nonce
      username <- IO.print("Username: ") *> IO.readLine
      password <- IO.print("Password: ") *> IO.readLine
      session  <- q.session(username, password, nonce)
      _ <- q
        .pdfsInSite(session)
        //.evalTap(q.downloadPdf(session))
        .evalTap(IO.println)
        .compile
        .drain
      _ <- backend.close()
    } yield ExitCode.Success
}
