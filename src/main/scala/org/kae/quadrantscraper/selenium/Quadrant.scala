package org.kae.quadrantscraper.selenium

import cats.Applicative
import cats.implicits.*
import java.time.Year
import sttp.model.Uri

trait Quadrant[F[*]: Applicative] {
  def pdfsForYear(year: Year): F[Set[Uri]]

  def pdfsByYear: F[Map[Year, Set[Uri]]] =
    val years = (1956 to 2021).map(Year.of).toList
    years
      .traverse(pdfsForYear)
      .map(years.zip(_).toMap)
}
