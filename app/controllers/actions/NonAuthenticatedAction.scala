package controllers.actions

import controllers.routes
import models.UserManagerModel
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}
import play.api.mvc.Results.Redirect

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonAuthenticatedAction @Inject()(parser: BodyParsers.Default)(implicit userManagerModel: UserManagerModel , executionContext: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], invokeRouteAction: Request[A] => Future[Result]): Future[Result] = {
      Authentication.validateSessionAuthentication {
        case AuthenticationSuccess(_) => Future.successful(Redirect(routes.HomeController.index))
        case AuthenticationFailure() => invokeRouteAction(request)
      }(request ,userManagerModel, executionContext)
  }
}
