package org.kae.quadrantscraper

import cats.data.NonEmptyList
import cats.effect.Resource
import cats.effect.kernel.Sync
import cats.implicits._
import java.nio.file.{Files, Paths}
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._
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

  def existingScrapeablePages(session: Session): fs2.Stream[F, Uri]

  def pdfsInPage(session: Session)(uri: Uri): fs2.Stream[F, Uri]

  def pdfsInSite(session: Session): fs2.Stream[F, Uri] =
    existingScrapeablePages(session)
      .flatMap(pdfsInPage(session))

  def downloadPdf(session: Session)(uri: Uri): F[Unit]
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
          response <- basicRequest
            .get(homePage)
            .send(backend)
            .ensure(new Exception("Home Page Get failed)"))(_.code.isSuccess)
          htmlText   <- Sync[F].fromEither(response.body.leftMap(new Exception(_)))
          loginNonce <- Quadrant.loginNonceInDoc[F](htmlText)
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
            .ensure(new Exception("login failed)"))(_.code.isSuccess)

          cookies = response.cookies.partitionMap(identity)._2.toList
          cookiesNel <- Sync[F].fromOption(
            NonEmptyList.fromList(cookies),
            new Exception("no cookies")
          )
        } yield Session(cookiesNel)
      }

      override def pdfsInPage(session: Session)(
          uri: Uri
      ): fs2.Stream[F, Uri] =
        fs2.Stream.evalSeq(pdfsInPageList(session)(uri))

      override def existingScrapeablePages(session: Session): fs2.Stream[F, Uri] =
        fs2.Stream
          .fromIterator(scrapeablePages.iterator, 1)
          .evalFilter(pageExists(session, _))

      override def downloadPdf(session: Session)(uri: Uri): F[Unit] =
        basicRequest
          .get(uri)
          .response(asByteArrayAlways)
          .cookies(session.cookies.toList)
          .send(backend)
          .ensure(new Exception("copy failed)"))(_.code.isSuccess)
          .flatMap { response =>
            Sync[F]
              .delay(
                Paths
                  .get(System.getProperty("user.home"))
                  .resolve("Downloads/QuadrantPdfs")
                  .resolve(uri.pathSegments.segments.last.v)
              )
              .flatMap { targetPath =>
                Sync[F].delay(Files.write(targetPath, response.body)).void
              }
          }

      private def pdfsInPageList(session: Session)(uri: Uri): F[List[Uri]] =
        for {
          response <- basicRequest
            .get(uri)
            .cookies(session.cookies.toList)
            .send(backend)
          htmlText <- Sync[F].fromEither(response.body.leftMap(new Exception(_)))
          pdfLinks <- pdfLinksInDoc(htmlText)
        } yield pdfLinks

      private def pageExists(
          session: Session,
          uri: Uri
      ): F[Boolean] =
        basicRequest
          .head(uri)
          .cookies(session.cookies.toList)
          .send(backend)
          .map(_.code.isSuccess)

    }

  private def loginNonceInDoc[F[_]: Sync](html: String): F[String] =
    Sync[F]
      .delay {
        val doc = Jsoup.parse(html)
        val elt = doc.select("#woocommerce-login-nonce")
        elt.attr("value")
      }
      .ensure(new Exception("no nonce)"))(_.nonEmpty)

  private def pdfLinksInDoc[F[_]: Sync](html: String): F[List[Uri]] =
    linksInDoc[F](html)
      .map(
        _.filter { href => href.endsWith(".pdf") || href.endsWith(".PDF") }
      )
      .map { _.map(homePage.withWholePath) }

  private def linksInDoc[F[_]: Sync](html: String): F[List[String]] =
    Sync[F].delay {
      Jsoup
        .parse(html)
        .select("a[href]")
        .asScala
        .map(_.attr("href"))
        .filter(_.nonEmpty)
        .toList
    }

  private def scrapeablePages = for {
    year  <- (2013 to 2021).toList
    month <- 1 to 12
  } yield scrapeablePage(year, month)

  private def scrapeablePage(year: Int, month: Int) =
    homePage.withWholePath(
      s"wp-content/uploads/$year/${month.formatted("%02d")}/"
    )
}
