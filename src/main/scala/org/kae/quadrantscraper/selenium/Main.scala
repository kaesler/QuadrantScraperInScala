package org.kae.quadrantscraper.selenium

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import org.openqa.selenium.chrome.ChromeDriver
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    IO(doStuff()) *>
      ExitCode.Success.pure[IO]

  private def doStuff(): Unit =
    val driver = ChromeDriver()
    driver.get("http://www.google.com/")
    Thread.sleep(5000)
