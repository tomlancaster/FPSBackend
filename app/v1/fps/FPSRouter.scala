package v1.fps

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.Logger

class FPSRouter @Inject()(userController: UserController, mediaController: MediaController) extends SimpleRouter {

  val logger = Logger(getClass)

  val prefix = "/v1/fps"

  def link(id: Long): String = {
    import io.lemonlabs.uri.dsl._
    val url = prefix / id.toString
    url.toString()
  }

  override def routes: Routes = {
    case GET(p"/users") =>
      userController.index

    case POST(p"/users") =>
      userController.register

    case GET(p"/users/$id") =>
      userController.show(id.toLong)

    case POST(p"/users/login") =>
      userController.login

    case GET(p"/media/stream/$id") =>
      mediaController.streamForUser(id.toLong)

    case GET(p"/media/$id") =>
      mediaController.getMedia(id.toLong)

    case POST(p"/media") =>
      logger.trace("in post media route")
      mediaController.postMedia
  }

}
