package controllers

import java.io.File
import java.nio.file.Path

import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{BodyParser, MultipartFormData, Request, RequestHeader}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.{FakeRequest, Injecting}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.http.HeaderNames
import play.api.libs.Files
import play.api.libs.Files.{ SingletonTemporaryFileCreator, TemporaryFile}
import play.api.mvc.MultipartFormData.FilePart
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class MediaControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {


  "MediaRouter" should {
    "not be able to upload file when file does not exist" in {
      val file = SingletonTemporaryFileCreator.create("", "")
      val part = FilePart[TemporaryFile](
        key = "file",
        filename = "foo",
        contentType = Some("image/png"),
        ref = file
      )
      val files = Seq[FilePart[TemporaryFile]](part)
      val formData = MultipartFormData(
        dataParts = Map(),
        files = Seq(part),
        badParts = Seq()
      )

      val request =
        FakeRequest(POST, "/v1/media")
          .withMultipartFormDataBody(formData)

      val response: Future[Result] = route(app, request).get
      status(response) mustBe BAD_REQUEST


    }
  }

}
