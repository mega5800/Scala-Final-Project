package controllers

import models.UserManagerModel
import play.api.libs.concurrent.Futures
import play.api.mvc._
import slick.jdbc.JdbcProfile
import services.MailerService
import javax.inject._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import play.api.libs.concurrent.Futures._

import java.sql.SQLException
import scala.concurrent.{Await, ExecutionContext, Future}

// TODO: handle exceptions thrown from Future
@Singleton
class UserController @Inject()(userManagerModel: UserManagerModel, mailerService: MailerService, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures)
  extends AbstractController(cc) {
  private val maximumTimeout = 3.seconds

  private val loginCredentialsChecker: LoginCredentialsChecker = new LoginCredentialsChecker()
  private val registerCredentialsChecker: RegisterCredentialsChecker = new RegisterCredentialsChecker()

  private var resultObject: Result = null
  private var resultFuture: Future[Result] = null
  private var errorMessageString: Option[String] = None

  def createUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    registerCredentialsChecker.UserName = credentials("username").head
    registerCredentialsChecker.Password = credentials("password").head
    registerCredentialsChecker.Email = credentials("email").head

    errorMessageString = registerCredentialsChecker.getRegisterCredentialsValidityErrorMessage()
    var result: Future[Result] = null

    if (errorMessageString.isEmpty) {
      //TODO: move this code into seprate method
      resultFuture = userManagerModel.createUser(registerCredentialsChecker.UserName, registerCredentialsChecker.Password, registerCredentialsChecker.Email)
        .withTimeout(maximumTimeout)
        .transformWith {
          case Success(userId) => Future.successful(Redirect(routes.HomeController.loginPage))
          case Failure(exception) => exception match {
            case sqlException: SQLException =>
              println(sqlException)
              Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> "Failed to create user"))
            case exception: Throwable =>
              println(exception.getMessage)
              Future.successful(InternalServerError("Oops, something went wrong!"))
          }
        }
      //end of new method
    }
    else {
      resultFuture = Future.successful(Redirect(routes.HomeController.registerPage).flashing("error" -> errorMessageString.get))
    }

    resultFuture
  }

  def validateUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    loginCredentialsChecker.UserName = credentials("username").head
    loginCredentialsChecker.Password = credentials("password").head

    errorMessageString = loginCredentialsChecker.getLoginCredentialsValidityErrorMessage()
    if (errorMessageString.isEmpty) {
      //TODO: move this code into seprate method
      resultFuture = userManagerModel.validateUser(loginCredentialsChecker.UserName, loginCredentialsChecker.Password).withTimeout(maximumTimeout).transformWith {
        case Success(userValidated) =>
          if (userValidated) {
            Future.successful(Redirect(routes.HomeController.index).withSession("username" -> loginCredentialsChecker.UserName))
          }
          else {
            Future.successful(Redirect(routes.HomeController.loginPage).flashing("error" -> "Wrong username or password"))
          }
        case Failure(exception) => exception match {
          case exception: Exception =>
            println(exception.getMessage)
            Future.successful(InternalServerError("Oops, something went wrong!"))
        }
      }
      //end of new method
    }
    else {
      resultFuture = Future.successful(Redirect(routes.HomeController.loginPage).flashing("error" -> errorMessageString.get))
    }

    resultFuture
  }


  def logout: Action[AnyContent] = Action {
    Redirect(routes.HomeController.loginPage).withNewSession
  }

  def forgotPasswordPage: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.forgotPasswordPage())
  }

  def passwordResetPage(passwordResetToken: String): Action[AnyContent] = Action.async {
    implicit request =>
      userManagerModel.isPasswordResetTokenValid(passwordResetToken).withTimeout(maximumTimeout).transformWith {
        case Success(isPasswordTokenValid) =>
          if (isPasswordTokenValid) {
            Future.successful(Ok(views.html.passwordResetPage(passwordResetToken)))
          }
          else {
            Future.successful(Forbidden("You cannot access this page."))
          }
        case Failure(exception) => exception match {
          case exception: Throwable =>
            println(exception.getMessage)
            Future.successful(InternalServerError("Oops, something went wrong!"))
        }
      }
  }

  def requestPasswordReset: Action[AnyContent] = Action.async {
    implicit request =>
      val email = request.body.asFormUrlEncoded.get("email").head

      userManagerModel.createPasswordResetToken(email).withTimeout(maximumTimeout).transformWith {
        case Success(passwordResetToken) =>
          val passwordResetLink: String = s"http://localhost:9000/passwordReset?token=${passwordResetToken}"
          mailerService.sendSimpleEmail(emailTo = email, subject = "ScalaProject - Password reset link", content = passwordResetLink)
          Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "Your request has been sent, if your email is in your systems you will receive an email shortly."))
        case Failure(exception) => exception match {
          case sqlException: SQLException =>
            println(sqlException)
            Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem, please try again later."))
          case exception: Throwable =>
            println(exception.getMessage)
            Future.successful(InternalServerError("Oops, something went wrong!"))
        }
      }
  }

  def resetPassword: Action[AnyContent] = Action.async {
    request =>
      val passwordResetData = request.body.asFormUrlEncoded.get
      val newPassword = passwordResetData("newPassword").head
      val confirmPassword = passwordResetData("confirmPassword").head
      val passwordResetToken = passwordResetData("passwordResetToken").head

      if (newPassword != confirmPassword) {
        Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "Passwords do not match."))
      }
      else {
        userManagerModel.updatePasswordWithPasswordResetToken(passwordResetToken, newPassword).withTimeout(maximumTimeout).transformWith {
          case Success(passwordChanged) =>
            if (passwordChanged) {
              Future.successful(Redirect(routes.HomeController.loginPage))
            }
            else {
              Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "There was a problem, please try again later."))
            }
          case Failure(exception) => exception match {
            case sqlException: SQLException =>
              println(sqlException)
              Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem, please try again later."))
            case exception: Throwable =>
              println(exception.getMessage)
              Future.successful(InternalServerError("Oops, something went wrong!"))
          }
        }
      }
  }
}
