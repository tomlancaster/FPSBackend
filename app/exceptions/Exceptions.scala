package exceptions

import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Gone, Unauthorized, Status, UnprocessableEntity}

import scala.collection.{GenTraversableOnce, Iterator, Map}

trait FPSError extends Throwable {
  def headers:List[(String,String)] = List(
    "Access-Control-Allow-Origin" -> "*"
  )
  def json:JsObject
  def res:Status
  def meta:JsObject = Json.obj()
  def httpResult:Result = res(
      json ++ meta
  ) .as(
    "application/json"
  ) .withHeaders(
    headers: _*
  )
}

/**
  * The default error message constructor
  */
case class HTTPError(userMessage:String,devMessage:String,status:String,code:Int,extraHeaders:List[(String,String)] = Nil)
  extends Exception(devMessage)
    with FPSError
{
  override def headers:List[(String,String)] = super.headers ++ extraHeaders

  override def json:JsObject = Json.obj(
    "userMessage" -> userMessage,
    "developerMessage" -> devMessage,
    "status" -> status,
    "code" -> code
  )

  override def res:Status = Status(code)
}

/**
  * A constructor for an authentication failure, using HTTPError.
  */
object AuthenticationFailure {
  def apply(reason:String):HTTPError =
    HTTPError(
      reason,
      "Authentication Failed",
      "UNAUTHORIZED",
      401
    )
}

/**
  * 666 response code designed to get anyone who sees this to ask questions
  * immediately.  This error should only be thrown if there's something __SEVERELY__
  * wrong with our data.  Reserve this for something worse than an ISE.
  */
object DataIntegrityError {
  def apply(reason:String, location:String):HTTPError =
    HTTPError(
      "Please inform FPS immediately of this problem and how you found it",
      "Data integrity problem: " + reason + " found at " + location,
      "ERROR OF THE BEAST",
      666
    )
}

/**
  * Access denied, using HTTPError.
  */
object AccessDenied {
  def apply(reason:String):HTTPError =
    HTTPError(
      reason,
      "Access is denied to users who don't have the correct permissions to perform a task.  " +
        "This usually means that the user is trying to manipulate or view data that belongs " +
        "to someone else, or they are trying to perform tasks with FPS that is outside " +
        "their domain of membership",
      "FORBIDDEN",
      403
    )
}

/**
  * Not Found error constructor, using HTTPError.
  */
object DoesNotExist {
  def apply(itemType:String, itemId:Long):HTTPError =
    HTTPError(
      "This " + itemType + " does not exist",
      "The item id provided (" + itemId + ") has no corresponding record.",
      "NOT FOUND",
      404
    )

  def apply(itemType:String, itemDesc:String): HTTPError =
    HTTPError(
      "This " + itemType + " does not exist",
      "The item identifier provided (" + itemDesc + ") has no corresponding record.",
      "NOT FOUND",
      404
    )
}

/**
  * Bad request error constructor, using HTTPError.
  */
object DoesNotCompute {
  def noJson:HTTPError = this.apply("You didn't provide JSON, or didn't mark the Content-Type as such")

  def apply(reason:String):HTTPError =
    HTTPError(
      "Something sent to FPS doesn't make much sense, check your request and try again",
      reason,
      "BAD REQUEST",
      400
    )
}

case class ValidationError(reason:String, elts:Map[String, List[String]] = Map.empty)
  extends Exception("Validation Error: " + reason)
    with Map[String,List[String]]
    with FPSError
{
  import Function._

  override def json:JsObject =
    HTTPError(
      reason,
      "There were issues with the (correctly structured) data you sent to FPS. See 'fields' for further details.",
      "UNPROCESSABLE ENTITY",
      422
    ) .json ++ Json.obj(
      "fields" -> Json.toJson(
        elts.map( tupled { (elem,problem) =>
          if (problem.length == 1) {
            elem -> Json.toJson(problem.head)
          } else {
            elem -> Json.toJson(problem)
          }
        }).toMap
      )
    )

  override def res:Status = UnprocessableEntity

  override def +[A >: List[String]](kv:(String,A)):ValidationError = {
    import helper.MatchViews._
    val (k,v) = kv
    v match {
      case StringList(valu) => this.copy(elts = elts + (k -> valu))
      case valu:String => this <+ (k -> valu)
      case _ => this <+ (k -> v.toString)
    }
  }

  def <+(kv: (String, String)):ValidationError = {
    val (key,problem) = kv
    elts.get(key).map({ existent =>
      this.copy(elts = elts + ((key, problem :: existent)))
    }).getOrElse({
      this.copy(elts = elts + ((key, List(problem))))
    })
  }

  override def ++[A >: List[String]](kvs:GenTraversableOnce[(String, A)]):ValidationError = {
    kvs.foldLeft(this)(_ + _)
  }

  def <<+(kvs:GenTraversableOnce[(String,String)]):ValidationError = {
    kvs.foldLeft(this)(_ <+ _)
  }

  override def -(k:String):ValidationError = this.copy(elts = elts - k)

  override def get(k:String):Option[List[String]] = elts.get(k)

  override def iterator:Iterator[(String,List[String])] = elts.iterator

  override def isEmpty:Boolean = elts.isEmpty
}

object SimpleISE {
  def apply(message:String):HTTPError =
    HTTPError(
      "There was a problem processing your request.",
      message,
      "INTERNAL SERVER ERROR",
      500
    )
}

object DuplicateEmailError {
  private val logger: Logger = Logger(getClass())
  def apply(email: String): HTTPError = {
    logger.debug("in DEE apply")
    HTTPError(
      "That email already exists in our database.",
      email,
      "BAD REQUEST",
      400
    )
  }
}

case class DuplicateEmailError(email: String) extends Exception("We already have an account with the email " + email)
  with FPSError
{
  override def json:JsObject = Json.obj(
    "userMessage" -> "Duplicate email address",
    "developerMessage" -> "Email address must be unique",
    "status" -> "Bad Request",
    "code" -> 400
  )

  override def res:Status = BadRequest
}

case class BadUsernameOrPasswordError(email: String) extends Exception("Unable to log you in with those credentials ")
  with FPSError
{
  override def json:JsObject = Json.obj(
    "userMessage" -> "Bad Username or Password",
    "developerMessage" -> "Bad Username or Password",
    "status" -> "Bad Request",
    "code" -> 400
  )

  override def res:Status = BadRequest
}


