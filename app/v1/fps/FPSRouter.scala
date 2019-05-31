package v1.fps

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class FPSRouter @Inject()(controller: UserController) extends SimpleRouter {

  val prefix = "/v1/fps"

  def link(id: Long): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id.toString
    url.toString()
  }

  override def routes: Routes = {
    case GET(p"/users") =>
      controller.index

    case POST(p"/users") =>
      controller.register

    case GET(p"/users/$id") =>
      controller.show(id.toLong)
  }

}
