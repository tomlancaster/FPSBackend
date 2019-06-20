package models

import java.time.ZonedDateTime

import anorm.{SQL, SqlParser, ~}
import SqlParser._
import javax.inject.Inject
import play.api.Logger
import play.api.db.DBApi
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

case class Media(
                id: Long,
                mediatype_id: Int,
                location: Option[String] = None,
                media_path: String,
                owner_id: Long,
                created_at: ZonedDateTime
                )

object Media {
  implicit val format: Format[Media] = Json.format
}

@javax.inject.Singleton
class MediaRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {
  private val logger = Logger(getClass)

  private val db = dbapi.database("default")

  private[models] val simple = long("id") ~
              int("mediatype_id") ~
              str("location").? ~
              str("media_path") ~
              long("owner_id") ~
              get[ZonedDateTime]("created_at") map {
    case i~m~l~p~o~c => Media(i, m, l, p, o, c)
  }

  def saveMedia(media: Media): Option[Media] = {
    logger.trace("saveMedia repo:")
    db.withConnection { implicit connection =>
      val Some(id:Long) = SQL(
        """
          |INSERT INTO media (mediatype_id, location, media_path, owner_id, created_at)
          |VALUES ({mediatype_id}, {location}, {media_path}, {owner_id}, {created_at})
        """.stripMargin)
        .on(
          "mediatype_id" -> 1,
          "location" -> "foo",
          "media_path" -> media.media_path,
          "owner_id" -> media.owner_id,
          "created_at" -> media.created_at
        )
        .executeInsert()
      findById(id)
    }
  }

  def findById(id:Long): Option[Media] = {
    logger.trace(s"find: $id")
    db.withConnection { implicit connection =>
      SQL(
        """
          |SELECT * FROM media WHERE id = {id}
        """.stripMargin)
        .on("id" -> id).as(simple.singleOpt)
    }
  }

  def findAll(): Iterable[Media] = {
    logger.trace("findAll: ")
    db.withConnection { implicit connection =>
      val media = SQL(
        """
          |SELECT * FROM media
        """.stripMargin)
        .as(simple.*)
      logger.debug("media: " + media.toString())
      media
    }
  }
}

case class MediaType(
                    id: Int,
                    name: String,
                    mime_type: Option[String]
                    )

@javax.inject.Singleton
class MediaTypeRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  private[models] val simple = int("id") ~ str("name") ~ get[Option[String]]("mime_type") map {
    case i~n~m => MediaType(i, n, m)
  }
}


case class MediaSubject(
                       id: Long,
                       subjecttype_id: Int,
                       name: String,
                       description: Option[String],
                       user_id: Option[Long]
                       )

@javax.inject.Singleton
class MediaSubjectRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  private[models] val simple = long("id") ~ int("subjecttype_id") ~ str("name") ~
              get[Option[String]]("description") ~ get[Option[Long]]("user_id") map {
    case i~s~n~d~u => MediaSubject(i, s, n, d, u)
  }
}
