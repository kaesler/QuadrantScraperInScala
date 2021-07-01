package org.kae.quadrantscraper.selenium

import munit.FunSuite
import cats.effect.IO

class DocRepoTest extends FunSuite {
  import cats.effect.unsafe.implicits.global

  val repo     = DocRepo.create[IO]
  val contents = repo.contents.unsafeRunSync()
  println(contents.size)
}
