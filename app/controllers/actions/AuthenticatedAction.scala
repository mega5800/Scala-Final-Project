package controllers.actions


import controllers.routes
import controllers.utilties.Attributes
import models.UserManagerModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedAction @Inject()(parser: BodyParsers.Default)(implicit userManagerModel: UserManagerModel ,executionContext: ExecutionContext)
  extends ActionBuilderImpl(parser){

  override def invokeBlock[A](request: Request[A], invokeRouteAction: Request[A] => Future[Result]): Future[Result] = {
    Authentication.validateSessionAuthentication {
      case AuthenticationSuccess(userId) =>
        val updatedRequest = request.addAttr(Attributes.UserID, userId)
        invokeRouteAction(updatedRequest)
      case AuthenticationFailure() =>
        Future.successful(Redirect(routes.HomeController.loginPage))
    }(request, userManagerModel, executionContext)

  }
}

