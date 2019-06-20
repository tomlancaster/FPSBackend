package services

import exceptions.{MediaSaveError}
import javax.inject.Inject
import models.{Media, MediaRepository}
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class MediaService @Inject()(mediaRepository: MediaRepository)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)


  def saveMedia(media: Media): Try[Media] = {
    logger.trace("saveMedia:")
    val maybeMedia = mediaRepository.saveMedia(media)
    maybeMedia match {
      case Some(media) => Success(media)
      case None => Failure(MediaSaveError())
    }
  }

}
