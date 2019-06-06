package services

import javax.inject.Inject
import models.{User, UserPublic, UserRepository}
import play.api.Logger
import v1.fps.{LoginUserFormInput, RegisterUserFormInput}
import exceptions.{BadUsernameOrPasswordError, DuplicateEmailError}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class UserService @Inject()(userRepo: UserRepository)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)


  def create(regUser: RegisterUserFormInput): Try[UserPublic] =  {
    logger.trace("create: ")
    logger.debug("in create")
    // check for duplicate emails
    if (userRepo.findByEmail(regUser.email).isDefined) {
      logger.debug("seen email")
      Failure(new DuplicateEmailError(regUser.email))
    } else {
      regUser.toUser match {
        case Success(newUser) => Success(userRepo.registerUser(newUser).get)
        case Failure(f) => Failure(new Exception("Unable to register user"))
      }
    }
  }

  def login(loginUser: LoginUserFormInput): Try[UserPublic] = {
    val maybeUser:Option[User] = userRepo.findByEmail(loginUser.email)
    maybeUser match {
      case Some(user) => {
        if (BCrypt.checkpw(loginUser.password, user.hashed_password)) {
          logger.debug("login success")
          Success(userRepo.findById(user.id.get).get)
        } else {
          logger.debug("login failure")
          Failure(new BadUsernameOrPasswordError(loginUser.email))
        }
      }
      case None => {
        logger.debug("user not found")
        Failure(new BadUsernameOrPasswordError(loginUser.email))
      }
    }
  }

  def findById(id: Long): Future[Option[UserPublic]] = Future(userRepo.findById(id))

  def findAll(): Future[Iterable[UserPublic]] = Future(userRepo.findAll())

}
