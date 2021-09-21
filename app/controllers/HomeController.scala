package controllers

import controllers.actions.{AuthenticatedAction, NonAuthenticatedAction}
import controllers.utilties.{Attributes, FutureResultHandler, FutureSuccess}
import models.CostsManagerModel
import play.api.libs.concurrent.Futures
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(authenticatedAction: AuthenticatedAction, nonAuthenticatedAction: NonAuthenticatedAction, costsManagerModel: CostsManagerModel, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures)
  extends AbstractController(cc) {
  def index: Action[AnyContent] = authenticatedAction.async { implicit request =>
    val userId = request.attrs(Attributes.UserID)
    val handler = FutureResultHandler(costsManagerModel.getAllCostsForUser(userId))

    handler.handle {
      case FutureSuccess(userItemCosts) => Future.successful(Ok(views.html.index(userItemCosts)))
    }
  }

  def loginPage: Action[AnyContent] = nonAuthenticatedAction { implicit request =>
    Ok(views.html.login())
  }

  def registerPage: Action[AnyContent] = nonAuthenticatedAction { implicit request =>
    Ok(views.html.register())
  }
}


