package v1.fps

import exceptions.{AccessDenied, AuthenticationFailure, DoesNotCompute, DoesNotExist, ValidationError}
import javax.inject.Inject
import models.{UserPublic, UserRepository}
import net.logstash.logback.marker.LogstashMarker
import play.api.{Logger, MarkerContext}
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.typedmap.{TypedKey, TypedMap}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A wrapped request for fps resources.
  *
  * This is commonly used to hold request-specific information like
  * security credentials, and useful shortcut methods.
  */
trait FPSRequestHeader
  extends MessagesRequestHeader
    with PreferredMessagesProvider

class FPSRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request)
    with FPSRequestHeader

class AuthenticatedFPSRequest[A](
                                 request: Request[A],
                                 val user: UserPublic,
                                 messagesApi: MessagesApi)
  extends FPSRequest(request, messagesApi)

/**
  * Provides an implicit marker that will show the request in all logger statements.
  */
trait RequestMarkerContext {
  import net.logstash.logback.marker.Markers

  private def marker(tuple: (String, Any)) = Markers.append(tuple._1, tuple._2)

  private implicit class RichLogstashMarker(marker1: LogstashMarker) {
    def &&(marker2: LogstashMarker): LogstashMarker = marker1.and(marker2)
  }

  implicit def requestHeaderToMarkerContext(
                                             implicit request: RequestHeader): MarkerContext = {
    MarkerContext {
      marker("id" -> request.id) && marker("host" -> request.host) && marker(
        "remoteAddress" -> request.remoteAddress)
    }
  }

}

/**
  * The action builder for the FPS resource.
  *
  * This is the place to put logging, metrics, to augment
  * the request with contextual data, and manipulate the
  * result.
  */
class FPSActionBuilder @Inject()(messagesApi: MessagesApi,
                                  playBodyParsers: PlayBodyParsers)(
                                   implicit val executionContext: ExecutionContext)
  extends ActionBuilder[FPSRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  type FPSRequestBlock[A] = FPSRequest[A] => Future[Result]

  private val logger = Logger(this.getClass)

  override def invokeBlock[A](request: Request[A],
                              block: FPSRequestBlock[A]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(
      request)
    logger.trace(s"invokeBlock: ")

    val future = block(new FPSRequest(request, messagesApi))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}

class AuthenticatedFPSActionBuilder @Inject()(messagesApi: MessagesApi,
                                              playBodyParsers: PlayBodyParsers,
                                              userRepository: UserRepository,
                                              override implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedFPSRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  type AuthenticatedFPSRequestBlock[A] = AuthenticatedFPSRequest[A] => Future[Result]

  private val logger = Logger(this.getClass)

  override def invokeBlock[A](request: Request[A],
                              block: AuthenticatedFPSRequestBlock[A]): Future[Result] = {

    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(
      request)
    logger.trace(s"invokeBlock(Authenticated): ")
    val future = request.session
      .get("userId")
      .flatMap {
        id: String => userRepository.findById(id.toLong)
      }
      .map(user => block(new AuthenticatedFPSRequest[A](request, user, messagesApi)))
      .getOrElse(Future.successful(Results.Forbidden))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}


/**
  * Packages up the component dependencies for the post controller.
  *
  * This is a good way to minimize the surface area exposed to the controller, so the
  * controller only has to have one thing injected.
  */
case class FPSControllerComponents @Inject()(
                                               fpsActionBuilder: FPSActionBuilder,
                                               authenticatedFPSActionBuilder: AuthenticatedFPSActionBuilder,
                                               actionBuilder: DefaultActionBuilder,
                                               parsers: PlayBodyParsers,
                                               messagesApi: MessagesApi,
                                               langs: Langs,
                                               fileMimeTypes: FileMimeTypes,
                                               executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

/**
  * Exposes actions and handler to the FPSController by wiring the injected state into the base class.
  */
class FPSBaseController @Inject()(pcc: FPSControllerComponents)
  extends BaseController
    with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = pcc

  def FPSAction: FPSActionBuilder = pcc.fpsActionBuilder

  def AuthenticatedFPSAction: AuthenticatedFPSActionBuilder = pcc.authenticatedFPSActionBuilder

  def dontKnow(thing:String):Result =
    DoesNotCompute(
      thing
    ) .httpResult

  def notAllowedTo(thing:String):Result =
    AccessDenied("You're not allowed to " + thing)
      .httpResult


  def shouldNotBeHere:Result =
    AuthenticationFailure("You're not logged in, please log in and try again")
      .httpResult

  def notFound(thing:String,id:Long):Result = DoesNotExist(thing,id).httpResult
  def notFound(thing:String,reason:String):Result = DoesNotExist(thing,reason).httpResult

  def validationError(errorMap: Map[String,String]):Result = {
    val r = ValidationError("Data given didn't pass validation") <<+ errorMap
    r.httpResult
  }

}

