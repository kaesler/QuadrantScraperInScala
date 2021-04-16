package org.kae.quadrantscraper

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      q = Quadrant.create(backend)

      nonce <- q.nonce
      _     <- IO.println(nonce)

      session <- q.session(
        "kevin.esler@gmail.com",
        "karakatana",
        nonce
      )
      _ <- IO.println(session)

      pages <- q.existingScrapeablePages(session)
      _ <- IO.println(
        pages.mkString("\n")
      )
    } yield ExitCode.Success
}
