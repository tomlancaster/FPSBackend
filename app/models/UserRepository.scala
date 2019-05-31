package models

import java.time.ZonedDateTime

import anorm.{SQL, SqlParser, ~}
import SqlParser._
import javax.inject.Inject
import play.api.Logger
import play.api.db.DBApi
import play.api.libs.json.{Format, Json}
import v1.fps.RegisterUserFormInput
import scala.util.{Try, Success, Failure}

import scala.concurrent.Future

case class User(
                id: Option[Long],
                email: String,
                name: String,
                phone: Option[String],
                is_admin: Boolean,
                hashed_password: String,
                created_at: ZonedDateTime
               )

object User {
  implicit val format: Format[User] = Json.format
}

@javax.inject.Singleton
class UserRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")
  private val logger = Logger(this.getClass)

  private[models] val simple = get[Option[Long]]("id") ~
                str("email") ~
                str("name") ~
                get[Option[String]]("phone") ~
                bool("is_admin") ~
                str("hashed_password") ~
                get[ZonedDateTime]("created_at") map {
    case i ~ e ~ n ~ p ~ a ~ h ~ c => User(i, e, n, p, a, h, c)

  }

  def create(regUser: RegisterUserFormInput): Future[Option[User]] = Future {
    logger.trace("register: ")
    regUser.toUser match {
      case Success(newUser) => registerUser(newUser)
      case Failure(f) => None
    }
  }

  private def registerUser(newUser: User): Option[User] = {
    db.withConnection { implicit connection =>
      val Some(id:Long) = SQL(
        """
          |INSERT INTO users (email, phone, name, hashed_password, is_admin, created_at)
          |VALUES ({email}, {phone}, {name}, {hashed_password}, {is_admin}, {created_at})
        """.stripMargin)
        .on(
          "email" -> newUser.email,
          "phone" -> newUser.phone,
          "name" -> newUser.name,
          "hashed_password" -> newUser.hashed_password,
          "is_admin" -> newUser.is_admin,
          "created_at" -> newUser.created_at)
        .executeInsert()
      SQL(
        """
          |SELECT * FROM users WHERE id = {id}
        """.stripMargin)
        .on("id" -> id).as(simple.singleOpt)
    }
  }

  def findById(id:Long): Future[Option[User]] = Future {
    logger.trace(s"find: $id")
    db.withConnection { implicit connection =>
      SQL(
        """
          |SELECT * FROM users WHERE id = {id}
        """.stripMargin)
        .on("id" -> id).as(simple.singleOpt)
    }
  }

  def findAll(): Future[Iterable[User]] = Future {
    logger.trace("findAll: ")
    db.withConnection { implicit connection =>
      val users = SQL(
        """
          |SELECT * FROM users
        """.stripMargin)
        .as(simple.*)
      logger.debug("users: " + users.toString())
      users
    }
  }
}



