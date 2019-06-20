package v1.fps

import java.time.ZonedDateTime

import exceptions.{DuplicateEmailError, BadUsernameOrPasswordError}
import javax.inject.Inject
import models.{User}
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api._
import play.api.i18n.Lang
import play.api.mvc._
import play.api.i18n.MessagesApi
import services.UserService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try,Success,Failure}

case class RegisterUserFormInput (email: String,
                                 phone: Option[String] = None,
                                 password: String,
                                 password_repeated: String,
                                 is_admin: Boolean,
                                 name: String) {
  def toUser: Try[User] = Try {
    User(
      id = None,
      email = email,
      name = name,
      phone = phone,
      hashed_password = BCrypt.hashpw(password, BCrypt.gensalt(12)),
      is_admin = is_admin,
      created_at = ZonedDateTime.now()
    )
  }
}

case class LoginUserFormInput (email: String, password: String)

class UserController @Inject()(cc: FPSControllerComponents, userService: UserService)(implicit ec: ExecutionContext)
  extends FPSBaseController(cc) {

  private val logger = Logger(getClass)
  private val lang = cc.langs.availables.head
  private val registerForm: Form[RegisterUserFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> email,
        "phone" -> optional(text),
        "password" -> text(minLength = 8),
        "password_repeated" -> text(minLength = 8),
        "is_admin" -> boolean,
        "name" -> nonEmptyText
      )(RegisterUserFormInput.apply)(RegisterUserFormInput.unapply).verifying(
        cc.messagesApi("registration.password_match_error")(lang),
        fields =>
          fields match {
            case userReg => userReg.password == userReg.password_repeated
          }
      )
    )
  }

  private val loginForm: Form[LoginUserFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText
      )(LoginUserFormInput.apply)(LoginUserFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = AuthenticatedFPSAction.async { implicit request =>
    logger.trace("index: ")
    userService.findAll.map { users =>
      Ok(Json.toJson(users))
    }
  }

  def register: Action[AnyContent] = FPSAction.async { implicit request =>
    logger.trace("process: ")
    logger.debug("register controller method")
    processRegistrationJsonPost()
  }

  def login: Action[AnyContent] = FPSAction.async { implicit request =>
    logger.trace("login: ")
    processLoginJsonPost()
  }

  def show(id: Long): Action[AnyContent] = FPSAction.async { implicit request =>
    logger.trace(s"show: id = $id")
    userService.findById(id).map { user =>
      user match {
        case Some(user) => Ok(Json.toJson(user))
        case None => notFound("User", id)
      }

    }
  }

  private def processRegistrationJsonPost[A]()(
    implicit request: FPSRequest[A]): Future[Result] = {
    def failure(badForm: Form[RegisterUserFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: RegisterUserFormInput) = {

      userService.create(input) match {
        case Success(user)  => Future.successful(Created(Json.toJson(user))
        .withSession("loggedIn" -> user.name, "id" -> user.id.toString))
        case Failure(exception) => exception match {
          case e: DuplicateEmailError => Future.successful(e.httpResult)
          case _:Exception => Future.successful(BadRequest("foo"))
        }
      }
    }

    registerForm.bindFromRequest().fold(failure, success)
  }


  private def processLoginJsonPost[A]()(
    implicit request: FPSRequest[A]): Future[Result] = {
    def failure(badForm: Form[LoginUserFormInput]) = {
      Future.successful(Unauthorized("Please provide an email and password"))
    }

    def success(input: LoginUserFormInput) = {
      userService.login(input) match {
        case Success(user) => Future.successful(Ok(Json.toJson(user)).withSession("loggedIn" -> user.name,
          "userId" -> user.id.toString))
        case Failure(exception) => exception match {
          case e: BadUsernameOrPasswordError => Future.successful(e.httpResult)
          case _: Exception => Future.successful(BadRequest("foo"))
        }
      }
    }
    loginForm.bindFromRequest().fold(failure, success)
  }


}