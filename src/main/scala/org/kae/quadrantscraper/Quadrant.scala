package org.kae.quadrantscraper

import cats.data.NonEmptyList
import cats.effect.kernel.Sync
import cats.effect.{Async, Resource}
import cats.implicits.*
import java.nio.file.{Files, Paths}
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.headers.CookieWithMeta
import sttp.model.{Header, Uri}

trait Quadrant[F[_]]:

  import Quadrant.*

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
end Quadrant

object Quadrant:

  import sttp.client3.*

  private val homePage = uri"https://quadrant.org.au/"

  private val userAgentHeader = Header.userAgent(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) " +
      "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15"
  )

  final case class NonceForLogin(s: String) extends AnyVal

  final case class Session(cookies: NonEmptyList[CookieWithMeta]) extends AnyVal

  sealed trait Error extends Throwable

  case object HomePageGetFailed extends Error

  case object LoginPostFailed extends Error

  case object NoCookies extends Error

  case object DownloadFailed extends Error

  def resource[F[_]: Async]: Resource[F, Quadrant[F]] =
    for
      backend <- Resource.make(AsyncHttpClientCatsBackend[F]())(_.close())
      q       <- Resource.liftK[F](create(backend).pure[F])
    yield q

  private def create[F[_]: Sync](backend: SttpBackend[F, Any]): Quadrant[F] =
    new Quadrant[F] {
      override def nonce: F[NonceForLogin] =
        for
          response <- basicRequest
            .headers(userAgentHeader)
            .get(homePage)
            .send(backend)
            .ensure(HomePageGetFailed)(_.code.isSuccess)
          htmlText <- Sync[F].fromEither(
            response.body
              .leftMap(new Exception(_))
          )
          loginNonce <- Quadrant.loginNonceInDoc[F](htmlText)
        yield {
          NonceForLogin(loginNonce)
        }

      override def session(
        username: String,
        password: String,
        nonceForLogin: NonceForLogin
      ): F[Session] =
        for
          response <- basicRequest
            .headers(userAgentHeader)
            .post(homePage)
            .body(
              Map(
                "username"                -> username,
                "password"                -> password,
                "woocommerce-login-nonce" -> nonceForLogin.s
              )
            )
            .send(backend)
            .ensure(LoginPostFailed)(_.code.isSuccess)

          cookies = response.cookies.partitionMap(identity)._2.toList
          cookiesNel <- Sync[F].fromOption(NonEmptyList.fromList(cookies), NoCookies)
        yield Session(cookiesNel)

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
          .headers(userAgentHeader)
          .get(uri)
          .response(asByteArrayAlways)
          .cookies(session.cookies.toList)
          .send(backend)
          .ensure(DownloadFailed)(_.code.isSuccess)
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
        for
          response <- basicRequest
            .headers(userAgentHeader)
            .get(uri)
            .cookies(session.cookies.toList)
            .send(backend)
          htmlText <- Sync[F].fromEither(
            response.body
              .leftMap(new Exception(_))
          )
          pdfLinks <- pdfLinksInDoc(htmlText)
        yield pdfLinks

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
        _.filter(_.toLowerCase.endsWith(".pdf"))
          .map(homePage.withWholePath)
      )

  private def linksInDoc[F[_]: Sync](html: String): F[List[String]] =
    Sync[F].delay(
      Jsoup
        .parse(html)
        .select("a[href]")
        .asScala
        .map(_.attr("href"))
        .filter(_.nonEmpty)
        .toList
    )

  private def scrapeablePages =
    for
      year  <- (2013 to 2021).toList
      month <- 1 to 12
    yield scrapeablePage(year, month)

  private def scrapeablePage(year: Int, month: Int) =
    homePage.withWholePath(
      s"wp-content/uploads/$year/${month.formatted("%02d")}/"
    )

end Quadrant
