package controllers

import controllers.actions.{AuthenticatedAction, NonAuthenticatedAction}
import controllers.utilties.CheckerTypes.CheckerTypes
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
  private val userNameChecker: TextChecker = new TextChecker(CheckerTypes.Username)
  private val passwordChecker: TextChecker = new TextChecker(CheckerTypes.Password)
  private val emailChecker: EmailChecker = new EmailChecker(CheckerTypes.Email)

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
    val credentials = request.body.asFormUrlEncoded.get
    userNameChecker.textValueToCheck = credentials("username").head
    passwordChecker.textValueToCheck = credentials("password").head
    emailChecker.textValueToCheck = credentials("email").head

    checkUserNameValidity()
    if (errorMessageString.isEmpty) {
      checkPasswordValidity(passwordChecker)
      if (errorMessageString.isEmpty) {
        checkEmailValidity()
        if (errorMessageString.isEmpty) {
          getResultForUserCreation(userNameChecker.textValueToCheck, passwordChecker.textValueToCheck, emailChecker.textValueToCheck)
        }
        else {
          Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> errorMessageString.get))
        }
      }
      else {
        Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> errorMessageString.get))
      }
    }
    else {
      Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> errorMessageString.get))
    }
  }

  private def checkUserNameValidity(): Unit = {
    errorMessageString = userNameChecker.getValidityErrorMessage()
  }

  private def checkPasswordValidity(passwordChecker: TextChecker): Unit = {
    errorMessageString = passwordChecker.getValidityErrorMessage()
  }

  private def checkEmailValidity(): Unit = {
    errorMessageString = emailChecker.getValidityErrorMessage()
  }

  def validateUser: Action[AnyContent] = nonAuthenticatedAction.async { request =>
    val credentials = request.body.asFormUrlEncoded.get
    userNameChecker.textValueToCheck = credentials("username").head
    passwordChecker.textValueToCheck = credentials("password").head

    println(userNameChecker.textValueToCheck)

    checkUserNameValidity()
    if (errorMessageString.isEmpty) {
      checkPasswordValidity(passwordChecker)
      if (errorMessageString.isEmpty) {
        getResultForUserValidation(userNameChecker.textValueToCheck, passwordChecker.textValueToCheck)
      }
      else {
        Future.successful(Redirect(routes.HomeController.loginPage).flashing("error" -> errorMessageString.get))
      }
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

  def requestPasswordReset: Action[AnyContent] = nonAuthenticatedAction.async { implicit request =>
    emailChecker.textValueToCheck = request.body.asFormUrlEncoded.get("email").head

    checkEmailValidity()
    if (errorMessageString.isEmpty) {
      getResultForPasswordResetRequest(emailChecker.textValueToCheck)
    }
    else {
      Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("error" -> errorMessageString.get))
    }
  }

  private def getResultForPasswordResetRequest(email: String): Future[Result] = {
    val handler = FutureResultHandler(userManagerModel.createPasswordResetToken(email))

    handler.handle {
      case FutureSuccess(passwordResetToken) =>
        val passwordResetLink: String = s"http://localhost:9000/passwordReset?token=$passwordResetToken"

        mailerService.sendSimpleEmail(emailTo = email, subject = "ScalaProject - Password reset link", content = passwordResetLink)
        Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "Your request has been sent! If your email is in your systems you will receive an email shortly."))
      case FutureFailure(exception) => exception match {
        case _: SQLException =>
          Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem please try again later."))
      }
    }
  }

  def resetPassword: Action[AnyContent] = nonAuthenticatedAction.async { request =>
    val confirmPasswordChecker: TextChecker = new TextChecker(CheckerTypes.Password)
    val passwordResetData = request.body.asFormUrlEncoded.get
    passwordChecker.textValueToCheck = passwordResetData("newPassword").head
    confirmPasswordChecker.textValueToCheck = passwordResetData("confirmPassword").head
    val passwordResetToken = passwordResetData("passwordResetToken").head


    if (passwordChecker.getValidityErrorMessage().isEmpty && confirmPasswordChecker.getValidityErrorMessage().isEmpty) {
      if (confirmPasswordChecker.textValueToCheck == passwordChecker.textValueToCheck) {
        getResultForPasswordReset(passwordResetToken, passwordChecker.textValueToCheck)
      }
      else {
        Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("error" -> s"Passwords dont match"))
      }
    }
    else {
      Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("error" -> s"Please enter 2 matching passwords with at least ${passwordChecker.minimalAmountOfChars} chars"))
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