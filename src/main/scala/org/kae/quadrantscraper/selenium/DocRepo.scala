package org.kae.quadrantscraper.selenium

import cats.effect.Sync
import java.io.File
import java.nio.file.Path

trait DocRepo[F[_]]:
  def contents: F[Set[DocId]]
end DocRepo

object DocRepo:
  private val root: Path = File(
    "/Users/kevinesler/Dropbox/3-Resources/Reading/Periodicals/Quadrant/"
  ).toPath

  def pathFor(docId: DocId): Path =
    root.resolve(s"${docId.year.toString}/${docId.fileName}")

  def docNotAlreadyDownloaded[F[_]: Sync](docId: DocId): F[Boolean] =
    summon[Sync[F]].blocking(!pathFor(docId).toFile.isFile)

end DocRepo
