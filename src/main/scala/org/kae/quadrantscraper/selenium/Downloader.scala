package org.kae.quadrantscraper.selenium

import cats.effect.Async
import cats.effect.kernel.{Resource, Sync}
import cats.implicits.*
import java.nio.file.Files
import org.typelevel.log4cats.Logger
import scala.concurrent.duration.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.quick.{asByteArrayAlways, basicRequest}
import sttp.client3.{RequestOptions, SttpBackend, SttpClientException}
import sttp.model.{Header, Uri}

trait Downloader[F[_]: Async]:
  def downloadDoc(docId: DocId, uri: Uri): F[Unit]
end Downloader

object Downloader:
  private val userAgentHeader = Header.userAgent(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) " +
      "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15"
  )

  def resource[F[_]: Async: Logger]: Resource[F, Downloader[F]] =
    for
      backend <- ArmeriaCatsBackend.resource[F]()
      q       <- Resource.liftK[F](create(backend).pure[F])
    yield q

  private def create[F[_]: Async: Logger](
    backend: SttpBackend[F, Any]
  ): Downloader[F] =
    new Downloader[F] {
      private val logger = summon[Logger[F]]

      def retry[A](n: Int)(fa: => F[A]): F[A] =
        fa.recoverWith { case ex: SttpClientException =>
          if n == 0 then summon[Async[F]].raiseError(ex)
          else retry(n - 1)(fa)
        }

      override def downloadDoc(docId: DocId, uri: Uri): F[Unit] =
        logger.info(s"Downloading $docId...") *>
          retry(3)(
            basicRequest
              .headers(userAgentHeader)
              .readTimeout(120.seconds)
              .get(uri)
              .response(asByteArrayAlways)
              .send(backend)
          )
            .ensure(DownloadFailed)(_.code.isSuccess)
            .flatMap { response =>
              summon[Sync[F]].blocking {
                val targetPath = DocRepo.pathFor(docId)
                Files.createDirectories(targetPath.getParent)
                Files.write(targetPath, response.body)
              }.void
            } *> logger.info(s"Downloading $docId...done")
    }

end Downloader
