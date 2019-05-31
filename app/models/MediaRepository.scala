package models

import java.time.ZonedDateTime

import anorm.{SQL, SqlParser, ~}
import SqlParser._
import javax.inject.Inject
import play.api.db.DBApi
import scala.concurrent.Future

case class Media(
                id: Long,
                mediatype_id: Int,
                location: Option[String] = None,
                media_path: String,
                owner_id: Long,
                created_at: ZonedDateTime
                )

@javax.inject.Singleton
class MediaRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  private[models] val simple = long("id") ~
              int("mediatype_id") ~
              str("location").? ~
              str("media_path") ~
              long("owner_id") ~
              get[ZonedDateTime]("created_at") map {
    case i~m~l~p~o~c => Media(i, m, l, p, o, c)
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
