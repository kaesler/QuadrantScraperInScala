package org.kae.quadrantscraper.selenium

import cats.effect.Sync
import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import java.time.Year
import java.util.function.BiPredicate
import scala.jdk.CollectionConverters.*

trait DocRepo[F[_]]:
  def pathFor(docId: DocId): Path
  def contents: F[Set[DocId]]
end DocRepo

object DocRepo:
  private val root: Path = File("/Users/kevinesler/Dropbox/Reading/Periodicals/Quadrant/").toPath

  def create[F[_]: Sync]: DocRepo[F] = new DocRepo[F] {
    override def pathFor(docId: DocId) =
      root.resolve(s"${docId.year.toString}/${docId.name}")

    override def contents: F[Set[DocId]] =
      summon[Sync[F]].delay(
        Files
          .find(
            root,
            2,
            new BiPredicate[Path, BasicFileAttributes] {
              override def test(
                path: Path,
                attrs: BasicFileAttributes
              ): Boolean =
                attrs.isRegularFile &&
                  path.getFileName.toString.endsWith(".pdf")
            }
          )
          .toList
          .asScala
          .map { path =>
            val name       = path.getFileName.toString
            val parentName = path.getParent.getFileName.toString
            val year       = Year.parse(parentName)
            DocId(year, name)
          }
          .toSet
      )
  }

end DocRepo
