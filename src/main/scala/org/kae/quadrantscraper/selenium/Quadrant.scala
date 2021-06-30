package org.kae.quadrantscraper.selenium

import cats.Applicative
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import java.time.Year
import org.openqa.selenium.By
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.typelevel.log4cats.Logger
import scala.jdk.CollectionConverters.*
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.Uri

trait Quadrant[F[_] : Applicative]:
  def pdfsForYear(year: Year): F[Set[Uri]]

  def pdfsByYear: F[Map[Year, Set[Uri]]] =
    val years = (Quadrant.firstYear to Year.now().getValue)
      .map(Year.of)
      .toList
    years
      .traverse(pdfsForYear)
      .map(years.zip(_).toMap)

end Quadrant

object Quadrant:
  private val firstYear = 1956

  import sttp.client3.*

  def resource[F[_]: Async: Logger](
      username: String,
      password: String
  ): Resource[F, Quadrant[F]] =
    for
      backend <- Resource.make(AsyncHttpClientCatsBackend[F]())(_.close())
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
      q       <- Resource.liftK[F](create[F](backend, chromeDriver, username, password))
    yield q

  def create[F[*]: Sync: Logger](
    backend: SttpBackend[F, Any],
    driver: ChromeDriver,
    username: String,
    password: String
  ): F[Quadrant[F]] =
    login[F](driver, username, password) *>
      new Quadrant[F] {
        val logger = summon[Logger[F]]

        override def pdfsForYear(year:  Year): F[Set[Uri]] = {
          summon[Sync[F]].delay {
            logger.info(s"Examining year $year")
            driver.get(s"https://quadrant.org.au/magazine/$year")
            Thread.sleep(2000)
            // val src = driver.getPageSource
            // println(src)
            driver
              .findElements(By.tagName("a"))
              .asScala
              .map(_.getAttribute("href"))
              .filter(_.endsWith(".pdf"))
              .flatMap(s => Uri.parse(s).toOption)
              .toSet
          }
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
