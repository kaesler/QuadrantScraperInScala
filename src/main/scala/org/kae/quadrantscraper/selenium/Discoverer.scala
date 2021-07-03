package org.kae.quadrantscraper.selenium

import cats.Applicative
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import java.nio.file.Files
import java.time.Year
import org.openqa.selenium.By
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.typelevel.log4cats.Logger
import scala.jdk.CollectionConverters.*
import sttp.model.Uri

/**
 * Discovers what documents are available and their URIs.
 * @tparam F the effect
 */
trait Discoverer[F[_] : Applicative]:
  def docsForYear(year: Year): F[Set[Uri]]

  def docsByYear: F[Map[Year, Set[Uri]]] =
    val years = (Discoverer.firstYear to Year.now().getValue)
      .map(Year.of)
      .toList
    years
      .traverse(docsForYear)
      .map(years.zip(_).toMap)

  def docUris: F[Map[DocId, Uri]] =
    docsByYear.map { pairs =>
      for
        (year, uris) <- pairs
        uri <- uris.toList
        name = uri.pathSegments.segments.last.v
      yield (DocId(year, name), uri)
    }

end Discoverer

object Discoverer:
  private val firstYear = 1956

  def resource[F[_]: Async: Logger](
      username: String,
      password: String
  ): Resource[F, Discoverer[F]] =
    for
      chromeDriver <- Resource.make(
        summon[Sync[F]].delay(
          ChromeDriver(
            ChromeOptions()
            .addArguments(
              "--headless",
              "--disable-gpu",
              "--window-size=1920,1200",
              "--ignore-certificate-errors"
            )
          )
        )
      )(driver => summon[Sync[F]].delay(driver.quit))
      q       <- Resource.liftK[F](create[F](chromeDriver, username, password))
    yield q

  private def create[F[*]: Sync: Logger](
    driver: ChromeDriver,
    username: String,
    password: String
  ): F[Discoverer[F]] =
    login[F](driver, username, password) *>
      new Discoverer[F] {
        val logger = summon[Logger[F]]

        override def docsForYear(year:  Year): F[Set[Uri]] =
          logger.info(s"Examining year $year") *>
          summon[Sync[F]].delay {
            driver.get(s"https://quadrant.org.au/magazine/$year")
            Thread.sleep(200)
            driver
              .findElements(By.tagName("a"))
              .asScala
              .map(_.getAttribute("href"))
              .filter(_.endsWith(".pdf"))
              .flatMap(s => Uri.parse(s).toOption)
              .toSet
          }
      }.pure[F]

  private def login[F[_]: Sync: Logger](
      driver: ChromeDriver,
      myUsername: String,
      myPassword: String
  ): F[Unit] =
    summon[Sync[F]].delay {
      driver.get("https://quadrant.org.au/my-account/")
      // TODO: do better
      Thread.sleep(1000)
      val username = driver.findElement(By.name("username"))
      username.sendKeys(myUsername)
      val password = driver.findElement(By.name("password"))
      password.sendKeys(myPassword)
      val button = driver.findElement(By.name("login"))
      button.click
      // TODO: do better by polling for the logged-in cookie
      Thread.sleep(1000)
    } *> summon[Logger[F]].info("Logged in")

end Discoverer