package org.kae.quadrantscraper

import scala.io.Source

import cats.data.NonEmptyList
import cats.effect.kernel.Sync
import org.jsoup.Jsoup
import sttp.model.Uri
import sttp.model.headers.CookieWithMeta
import scala.jdk.CollectionConverters._

trait Quadrant[F[_]] {

  import Quadrant._

  def nonce: F[NonceForLogin]

  def session(nonceForLogin: NonceForLogin): F[Session]

  def pdfs(session: Session): F[List[Uri]]
}

object Quadrant {
  final case class NonceForLogin(s: String) extends AnyVal

  final case class Session(cookies: NonEmptyList[CookieWithMeta]) extends AnyVal

  def extractLoginNonce[F[_]: Sync](homePageText: String): F[String] =
    Sync[F].delay {
      val doc = Jsoup.parse(homePageText)
      val elt = doc.select("#woocommerce-login-nonce")
      elt.attr("value")
    }

  def extractPdfs[F[_]: Sync](magazineYearPageText: String): F[List[String]] =
    Sync[F].delay {
//      val text = Source
//        .fromResource("magazine__1975.html")
//        .getLines()
//        .mkString("\n")
      Jsoup
        .parse(magazineYearPageText)
        .select("a[href]")
        .asScala
        .map(_.attr("href"))
        .filter(_.nonEmpty)
        .toList
    }

  def create[F[_]]: Quadrant[F] = new Quadrant[F] {
    override def nonce: F[NonceForLogin] = ???

    override def session(
        nonceForLogin: NonceForLogin
    ): F[Session] = ???

    override def pdfs(
        session: Session
    ): F[List[Uri]] = ???
  }
}
