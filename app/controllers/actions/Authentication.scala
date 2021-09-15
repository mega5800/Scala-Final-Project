package controllers.actions

import controllers.utilties.Attributes
import models.UserManagerModel
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{RequestHeader, Result}
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

object Authentication {
  def validateSessionAuthentication(onAuthentication: PartialFunction[Authentication, Future[Result]])(implicit request: RequestHeader, userManagerModel: UserManagerModel, executionContext: ExecutionContext): Future[Result] = async {
    request.session.get("userSession") match {
      case Some(userSessionToken) =>
        val userIdOption = await(userManagerModel.getUserIdBySessionToken(userSessionToken))
        if (userIdOption.nonEmpty) {
          await(onAuthentication(AuthenticationSuccess(userIdOption.get)))
        } else {
          await(onAuthentication(AuthenticationFailure())).withNewSession
        }
      case None => await(onAuthentication(AuthenticationFailure()))
    }
  }
}

abstract class Authentication

final case class AuthenticationSuccess(userId: Int) extends Authentication

final case class AuthenticationFailure() extends Authentication

