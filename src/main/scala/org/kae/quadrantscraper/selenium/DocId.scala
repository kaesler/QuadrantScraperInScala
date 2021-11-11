package org.kae.quadrantscraper.selenium

import java.time.Year

case class DocId(
  year: Year,
  fileName: String
)

object DocId:
  given Ordering[DocId] = Ordering.by(i => (i.year, i.fileName))
