package services

import javax.inject.Inject
import models.{User, UserRepository}
import play.api.Logger
import v1.fps.RegisterUserFormInput
import exceptions.DuplicateEmailError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class UserService @Inject()(userRepo: UserRepository)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)


  def create(regUser: RegisterUserFormInput): Try[User] =  {
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

  def findById(id: Long): Future[Option[User]] = Future(userRepo.findById(id))

  def findAll(): Future[Iterable[User]] = Future(userRepo.findAll())

}
