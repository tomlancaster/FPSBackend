package v1.fps

import java.time.ZonedDateTime

import akka.stream.alpakka.s3.MultipartUploadResult
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, Handler, _}
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
import services.{AwsS3Client, MediaService}
import java.util.UUID.randomUUID

import exceptions.MediaSaveError

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import models.Media
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

@Singleton
class MediaController @Inject()(cc: FPSControllerComponents,
                                awsS3Client: AwsS3Client,
                                mediaService: MediaService)
                               (implicit ec: ExecutionContext)
  extends FPSBaseController(cc) {

  private val logger = Logger(getClass)


  /*
  def upload = Action(toS3("fpd-dev")) { implicit request =>
    logger.debug("in postMedia")

    Ok("Uploaded")
  }
  */

  def getMedia(id: Long): Handler = ???

  def streamForUser(id: Long): _root_.play.api.mvc.Handler = ???
/*

  def toS3(bucket: String): BodyParser[Future[MultipartUploadResult]] = BodyParser { req =>
    Accumulator.source[ByteString].mapFuture { source =>
      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] = S3.multipartUpload(bucket, "myfile")
      val result: Future[s3.MultipartUploadResult] = source.runWith(s3Sink)(materializer)
      result, Right(result)
    }
  }

 */

  def postMedia: Action[MultipartFormData[MultipartUploadResult]] =
    AuthenticatedFPSAction.async(parse.multipartFormData(handleFilePartAwsUploadResult)) { implicit request =>
      val maybeUploadResult = request.body.file("file").map {
        case FilePart(key, filename, contentType, multipartUploadResult, _, _) => multipartUploadResult
      }

      def success(uploadResult: MultipartUploadResult) = {
        val media = Media(0, 1, Some("foo"), uploadResult.location.toString, request.session.get("userId").asInstanceOf[Long], ZonedDateTime.now())
        mediaService.saveMedia(media) match {
          case Success(media:Media) => Future.successful(Created(Json.toJson(media)))
          case Failure(exception) => exception match {
            case e: MediaSaveError => Future.successful(e.httpResult)
            case _: Exception => Future.successful(BadRequest("foo"))
          }
        }
      }

      maybeUploadResult.fold(
        Future.successful(InternalServerError("something went wrong"))
      )(res =>
        success(res)
      )

  }

  private def handleFilePartAwsUploadResult: FilePartHandler[MultipartUploadResult] = {
    case FileInfo(partName, filename, contentType, dispositionType) =>
      logger.debug("got FileInfo:")
      val randomName = randomUUID().toString + ".png"
      val sink = awsS3Client.s3Sink("fps-dev", randomName)
      val accumulator = Accumulator(sink)

      accumulator map { multipartUploadResult =>
        FilePart(partName, randomName, contentType, multipartUploadResult, -1, "foo")
      }
  }

}
