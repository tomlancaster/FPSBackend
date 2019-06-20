package services

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

@Singleton
class AwsS3Client @Inject()(system: ActorSystem) {

  def s3Sink(bucketName: String, bucketKey: String): Sink[ByteString, Future[MultipartUploadResult]] =
    S3.multipartUpload(bucketName, bucketKey)

}