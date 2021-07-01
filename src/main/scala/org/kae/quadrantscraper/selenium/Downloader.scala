package org.kae.quadrantscraper.selenium

import cats.effect.Async
import cats.effect.kernel.{Resource, Sync}
import cats.implicits.*
import java.nio.file.Files
import org.typelevel.log4cats.Logger
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.quick.{asByteArrayAlways, basicRequest}
import sttp.model.{Header, Uri}

trait Downloader [F[_] : Async]:
  def downloadDoc(docId: DocId, uri: Uri): F[Unit]

  def downloadDocs(refs: List[(DocId, Uri)]): F[Unit] =
    refs
      .traverse(downloadDoc.tupled)
      .void

end Downloader

object Downloader:
  private val userAgentHeader = Header.userAgent(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) " +
      "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15"
  )

  def resource[F[_]: Async: Logger]: Resource[F, Downloader[F]] =
    for
      backend <- Resource.make(AsyncHttpClientCatsBackend[F]())(_.close())
      q       <- Resource.liftK[F](create(backend).pure[F])
    yield q

  private def create[F[_]: Async: Logger](
    backend: SttpBackend[F, Any],
  ): Downloader[F] =
  new Downloader[F] {
    private val logger = summon[Logger[F]]

    override def downloadDoc(docId: DocId, uri: Uri): F[Unit] =
      logger.info(s"Downloading $docId...") *>
      basicRequest
        .headers(userAgentHeader)
        .get(uri)
        .response(asByteArrayAlways)
        .send(backend)
        .ensure(DownloadFailed)(_.code.isSuccess)
        .flatMap { response =>
          val targetPath = DocRepo.pathFor(docId)
          summon[Sync[F]].delay {
            Files.createDirectories(targetPath.getParent)
            Files.write(targetPath, response.body)
          }.void
        } *> logger.info(s"Downloading $docId...done")

  }

end Downloader

