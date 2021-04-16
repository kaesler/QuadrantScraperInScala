package org.kae.quadrantscraper

import scala.jdk.CollectionConverters._

import cats.data.NonEmptyList
import cats.effect.Resource
import cats.effect.kernel.Sync
import cats.implicits._
import org.jsoup.Jsoup
import sttp.model.Uri
import sttp.model.headers.CookieWithMeta

trait Quadrant[F[_]] {

  import Quadrant._

  def nonce: F[NonceForLogin]

  def session(
      username: String,
      password: String,
      nonceForLogin: NonceForLogin
  ): F[Session]

  def existingScrapeablePages(
      session: Session
  ): F[List[Uri]]

  def pdfs(session: Session): F[List[Uri]]
}

object Quadrant {
  import sttp.client3._

  private val homePage = uri"https://quadrant.org.au/"

  final case class NonceForLogin(s: String) extends AnyVal

  final case class Session(cookies: NonEmptyList[CookieWithMeta]) extends AnyVal

  def resource[F[_]: Sync]: Resource[F, Quadrant[F]] = ???

  def create[F[_]: Sync](backend: SttpBackend[F, Any]): Quadrant[F] =
    new Quadrant[F] {
      override def nonce: F[NonceForLogin] =
        for {
          response   <- basicRequest.get(homePage).send(backend)
          htmlText   <- Sync[F].fromEither(response.body.leftMap(new Exception(_)))
          loginNonce <- Quadrant.extractLoginNonce[F](htmlText)
        } yield NonceForLogin(loginNonce)

      override def session(
          username: String,
          password: String,
          nonceForLogin: NonceForLogin
      ): F[Session] = {
        for {
          response <- basicRequest
            .post(homePage)
            .body(
              Map(
                "username"                -> username,
                "password"                -> password,
                "woocommerce-login-nonce" -> nonceForLogin.s
              )
            )
            .send(backend)
          cookies = response.cookies.partitionMap(identity)._2.toList
          cookiesNel <- Sync[F].fromOption(
            NonEmptyList.fromList(cookies),
            new Exception("no cookies")
          )
        } yield Session(cookiesNel)
      }

      override def existingScrapeablePages(session: Session): F[List[Uri]] =
        scrapeablePages
          .traverse { uri =>
            pageExists[F](backend, session, uri).map { exists =>
              if (exists) uri.some else None
            }
          }
          .map(_.flatten)

      override def pdfs(
          session: Session
      ): F[List[Uri]] = ???
    }

  private def extractLoginNonce[F[_]: Sync](html: String): F[String] =
    Sync[F].delay {
      val doc = Jsoup.parse(html)
      val elt = doc.select("#woocommerce-login-nonce")
      elt.attr("value")
    }

  private def extractPdfLinks[F[_]: Sync](html: String): F[List[String]] =
    extractLinks[F](html).map(_.filter(_.endsWith(".pdf")))

  private def extractLinks[F[_]: Sync](html: String): F[List[String]] =
    Sync[F].delay {
      Jsoup
        .parse(html)
        .select("a[href]")
        .asScala
        .map(_.attr("href"))
        .filter(_.nonEmpty)
        .toList
    }

  def scrapeablePages = for {
    year  <- (2013 to 2021).toList
    month <- 1 to 12
  } yield scrapeablePage(year, month)

  def scrapeablePage(year: Int, month: Int) = {
    val fmt = month.formatted("%02d")
    homePage.withWholePath(s"wp-content/uploads/$year/$fmt/")
  }

  def pageExists[F[_]: Sync](
      backend: SttpBackend[F, Any],
      session: Session,
      uri: Uri
  ): F[Boolean] =
    basicRequest
      .head(uri)
      .cookies(session.cookies.toList)
      .send(backend)
      .map(_.code.isSuccess)
}
