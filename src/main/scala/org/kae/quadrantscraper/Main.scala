package org.kae.quadrantscraper

import java.time.Year

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp {
  private val homePage = uri"https://quadrant.org.au/"

  private def yearPage(year: Year) = homePage.withWholePath(s"magazine/${year.getValue}/")

  // TODO: close the backend
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      homePageGet = basicRequest.get(homePage)
      response1  <- homePageGet.send(backend)
      html1      <- IO.fromEither(response1.body.leftMap(new Exception(_)))
      loginNonce <- Quadrant.extractLoginNonce[IO](html1)
      _          <- IO.println(loginNonce)

      loginPost = basicRequest
        .post(homePage)
        .body(
          Map(
            "username"                -> "kevin.esler@gmail.com",
            "password"                -> "karakatana",
            "woocommerce-login-nonce" -> loginNonce
          )
        )
      response2 <- loginPost.send(backend)
      cookies = response2.cookies.partitionMap(identity)._2.toList
      _ <- IO.println(cookies)

      yearPageGet = basicRequest.get(yearPage(Year.of(2020))).cookies(cookies)
      response3 <- yearPageGet.send(backend)
      html3     <- IO.fromEither(response3.body.leftMap(new Exception(_)))
      _         <- IO.println(html3)
    } yield ExitCode.Success
  }
}
