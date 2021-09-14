package controllers

import models.{CostsManagerModel, UserManagerModel}
import play.api.libs.concurrent.Futures
import play.api.mvc._

import java.sql.SQLException
import javax.inject._
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(userManagerModel: UserManagerModel, costsManagerModel: CostsManagerModel, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures) extends AbstractController(cc) {
  val items: Seq[String] = (1 to 10).map(num => s"Item$num").toList

  // TODO: Abstract this into its own class along with Authentication classes
  def withAuthenticatedSession(onAuthentication: PartialFunction[Authentication, Future[Result]])(implicit request: Request[AnyContent]): Future[Result] = async {
    request.session.get("username") match {
      case Some(username) =>
        val userExists = await(userManagerModel.userExists(username))
        if (userExists) {
          await(onAuthentication(AuthenticationSuccess(username)))
        } else {
          await(onAuthentication(AuthenticationFailure())).withNewSession
        }
      case None => await(onAuthentication(AuthenticationFailure()))
    }
  }

  def index: Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedSession {
      case AuthenticationSuccess(username) =>
        val handler = FutureResultHandler(costsManagerModel.getCostsForUser(username))

        handler.handle(
          onSuccess = userCosts => Future.successful(Ok(views.html.index(userCosts))),
          onFailure = {
            case sqlException: SQLException => Future.successful(Redirect(routes.HomeController.loginPage))
          })
      case AuthenticationFailure() => Future.successful(Redirect(routes.HomeController.loginPage))
    }
  }

  def loginPage: Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedSession {
      case AuthenticationSuccess(username) => Future.successful(Redirect(routes.HomeController.index))
      case AuthenticationFailure() => Future.successful(Ok(views.html.login()))
    }
  }

  def registerPage: Action[AnyContent] = Action.async { implicit request =>
    withAuthenticatedSession {
      case AuthenticationSuccess(username) => Future.successful(Redirect(routes.HomeController.index))
      case AuthenticationFailure() => Future.successful(Ok(views.html.register()))
    }
  }
}
