package controllers

import controllers.actions.{AuthenticatedAction, NonAuthenticatedAction}
import controllers.utilties._
import models.UserManagerModel
import play.api.libs.concurrent.Futures
import play.api.mvc._
import services.MailerService

import java.sql.SQLException
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(authenticatedAction: AuthenticatedAction, nonAuthenticatedAction: NonAuthenticatedAction, userManagerModel: UserManagerModel, mailerService: MailerService, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures)
  extends AbstractController(cc) {

  private val loginCredentialsChecker: LoginCredentialsChecker = new LoginCredentialsChecker()
  private val registerCredentialsChecker: RegisterCredentialsChecker = new RegisterCredentialsChecker()

  private var errorMessageString: Option[String] = None

  private def getResultForUserCreation(username: String, password: String, email: String): Future[Result] = {
    val handler: FutureResultHandler[Int] = FutureResultHandler(userManagerModel.createUser(username, password, email))

    handler.handle {
      case FutureSuccess(_) => Future.successful(Redirect(routes.HomeController.loginPage))
      case FutureFailure(exception) => exception match {
        case _: SQLException =>
          Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> "Failed to create user"))
      }
    }
  }

  private def getResultForUserValidation(username: String, password: String): Future[Result] = {
    val handler = FutureResultHandler(userManagerModel.validateUser(username, password))

    handler.handle {
      case FutureSuccess(userSessionToken) =>
        if (userSessionToken.nonEmpty) {
          Future.successful(Redirect(routes.HomeController.index).withSession("userSession" -> userSessionToken.get))
        }
        else {
          Future.successful(Redirect(routes.HomeController.loginPage).flashing("error" -> "Wrong username or password"))
        }
    }
  }

  def createUser: Action[AnyContent] = nonAuthenticatedAction.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    registerCredentialsChecker.UserName = credentials("username").head
    registerCredentialsChecker.Password = credentials("password").head
    registerCredentialsChecker.Email = credentials("email").head

    errorMessageString = registerCredentialsChecker.getRegisterCredentialsValidityErrorMessage()

    if (errorMessageString.isEmpty) {
      getResultForUserCreation(registerCredentialsChecker.UserName, registerCredentialsChecker.Password, registerCredentialsChecker.Email)
    }
    else {
      Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> errorMessageString.get))
    }
  }

  def validateUser: Action[AnyContent] = nonAuthenticatedAction.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    loginCredentialsChecker.UserName = credentials("username").head
    loginCredentialsChecker.Password = credentials("password").head

    errorMessageString = loginCredentialsChecker.getLoginCredentialsValidityErrorMessage()
    if (errorMessageString.isEmpty) {
      getResultForUserValidation(loginCredentialsChecker.UserName, loginCredentialsChecker.Password)
    }
    else {
      Future.successful(Redirect(routes.HomeController.loginPage).flashing("error" -> errorMessageString.get))
    }
  }


  def logout: Action[AnyContent] = authenticatedAction { request =>
    val userId = request.attrs(Attributes.UserID)
    userManagerModel.deleteUserSession(userId)

    Redirect(routes.HomeController.loginPage).withNewSession
  }

  def forgotPasswordPage: Action[AnyContent] = nonAuthenticatedAction { implicit request =>
    Ok(views.html.forgotPasswordPage())
  }

  def passwordResetPage(passwordResetToken: String): Action[AnyContent] = nonAuthenticatedAction.async { implicit request =>
    FutureResultHandler(userManagerModel.isPasswordResetTokenValid(passwordResetToken)).handle {
      case FutureSuccess(isPasswordTokenValid) =>
        if (isPasswordTokenValid) {
          Future.successful(Ok(views.html.passwordResetPage(passwordResetToken)))
        }
        else {
          Future.successful(Forbidden("You cannot access this page."))
        }
    }
  }

  // TODO: validate email input field
  def requestPasswordReset: Action[AnyContent] = nonAuthenticatedAction.async { implicit request =>
    val email = request.body.asFormUrlEncoded.get("email").head

    getResultForPasswordResetRequest(email)
  }

  private def getResultForPasswordResetRequest(email: String): Future[Result] = {
    val handler = FutureResultHandler(userManagerModel.createPasswordResetToken(email))

    handler.handle {
      case FutureSuccess(passwordResetToken) =>
        val passwordResetLink: String = s"http://localhost:9000/passwordReset?token=$passwordResetToken"

        mailerService.sendSimpleEmail(emailTo = email, subject = "ScalaProject - Password reset link", content = passwordResetLink)
        Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "Your request has been sent, if your email is in your systems you will receive an email shortly."))
      case FutureFailure(exception) => exception match {
        case _: SQLException =>
          Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem, please try again later."))
      }
    }
  }

  // TODO: validate input fields newPassword and confirmPassword
  def resetPassword: Action[AnyContent] = nonAuthenticatedAction.async { request =>
    val passwordResetData = request.body.asFormUrlEncoded.get
    val newPassword = passwordResetData("newPassword").head
    val confirmPassword = passwordResetData("confirmPassword").head
    val passwordResetToken = passwordResetData("passwordResetToken").head

    if (newPassword != confirmPassword) {
      Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "Passwords do not match."))
    }
    else {
      getResultForPasswordReset(passwordResetToken, newPassword)
    }
  }

  private def getResultForPasswordReset(passwordResetToken: String, newPassword: String): Future[Result] = {
    val handler = FutureResultHandler(userManagerModel.updatePasswordWithPasswordResetToken(passwordResetToken, newPassword))

    handler.handle {
      case FutureSuccess(passwordChanged) =>
        if (passwordChanged) {
          Future.successful(Redirect(routes.HomeController.loginPage))
        }
        else {
          Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "There was a problem, please try again later."))
        }
      case FutureFailure(exception) => exception match {
        case _: SQLException =>
          Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem, please try again later."))
      }
    }
  }

}
