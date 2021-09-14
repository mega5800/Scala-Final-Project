package controllers

import play.api.mvc._
import javax.inject._

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val items: Seq[String] = (1 to 10).map(num => s"Item$num").toList

  def index: Action[AnyContent] = Action { request =>
    val usernameSession = request.session.get("username")
    usernameSession.map { username =>
      // TODO: grab username costs and send them to index
      Ok(views.html.index(items))
    }.getOrElse(Redirect(routes.HomeController.loginPage))
  }

  def loginPage: Action[AnyContent] = Action { implicit request =>
    val usernameSession = request.session.get("username")
    usernameSession.map { username =>
      Redirect(routes.HomeController.index)  
      }.getOrElse(Ok(views.html.login()))
  }

  def registerPage: Action[AnyContent] = Action { implicit request =>
    val usernameSession = request.session.get("username")
    usernameSession.map { _ =>
      Redirect(routes.HomeController.index)  
    }.getOrElse(Ok(views.html.register()))
  }
}
