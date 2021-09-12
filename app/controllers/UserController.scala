package controllers

import models.UserManagerModel
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile
import javax.inject._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.mailer._
import play.api.mvc._

class UserController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, mailerClient: MailerClient, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
 extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  private val userManagerModel: UserManagerModel = new UserManagerModel(db)

  def createUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    val username = credentials("username").head
    val password = credentials("password").head
    val email = credentials("email").head

    // assume it is empty
    var result: Result = Redirect(routes.HomeController.registerPage).flashing("error" -> "username and password fields must be provided")

    if(username.nonEmpty && password.nonEmpty && checkEmailValidity(email))
    {
      //checkEmailValidity will return {OK, emptyString, InvaludString}
        val userCreated = Await.result(userManagerModel.createUser(username, password, email), 5.seconds)

        if(userCreated)
        {
            result = Redirect(routes.HomeController.loginPage)
        }
        else
        {
            result = Redirect(routes.HomeController.registerPage).flashing("error" -> "Failed to create user.")
        }
    }

    Future(result)
  } 

  def checkEmailValidity(emailToCheck:String):Boolean =
    {
      if (emailToCheck.nonEmpty) false

      val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

      def check(e: String): Boolean = e match
      {
        case null                                           => false
        case e if e.trim.isEmpty                            => false
        case e if emailRegex.findFirstMatchIn(e).isDefined  => true
        case _                                              => false
      }

      check(emailToCheck)
    }

  def validateUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    val username = credentials("username").head
    val password = credentials("password").head

    // assume it is empty
    var result: Result = Redirect(routes.HomeController.loginPage).flashing("error" -> "username and password fields must be provided")

    if(username.nonEmpty && password.nonEmpty)
    {
        val userValidated = Await.result(userManagerModel.validateUser(username, password), 5.seconds)

        if(userValidated)
        {
            result = Redirect(routes.HomeController.index).withSession("username" -> username)
        }
        else
        {
            result = Redirect(routes.HomeController.loginPage).flashing("error" -> "Wrong username or password")
        }
    }

    Future(result)
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.HomeController.loginPage).withNewSession
   }

  def forgotPasswordPage: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.forgotPasswordPage())
  }

  def passwordResetPage(passwordResetToken: String): Action[AnyContent] = Action.async { implicit request =>
    val isPasswordTokenValid = Await.result(userManagerModel.isPasswordResetTokenValid(passwordResetToken), 3.seconds)

    if(isPasswordTokenValid){
      Future.successful(Ok(views.html.passwordResetPage(passwordResetToken)))
    }
    else{
      Future.successful(Forbidden("You cannot access this page."))
    }
  }

  def requestPasswordReset: Action[AnyContent] = Action.async { implicit request =>
    val email = request.body.asFormUrlEncoded.get("email").head
    val passwordResetToken = Await.result(userManagerModel.createPasswordResetToken(email), 3.seconds)

    if(passwordResetToken.nonEmpty){
      sendPasswordTokenResetLinkToEmail(passwordResetToken.get, email)
      Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "Your request has been sent, if your email is in your systems you will receive an email shortly."))
    }
    else{
      Future.successful(Redirect(routes.UserController.forgotPasswordPage).flashing("message" -> "There is a problem, please try again later."))
    }
  }

  def resetPassword: Action[AnyContent] = Action.async { request =>
    val passwordResetData = request.body.asFormUrlEncoded.get
    val newPassword = passwordResetData("newPassword").head
    val confirmPassword = passwordResetData("confirmPassword").head
    val passwordResetToken = passwordResetData("passwordResetToken").head

    if(newPassword != confirmPassword) {
      Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "Passwords do not match."))
    }
    else{
      val passwordChanged = Await.result(userManagerModel.resetPassword(passwordResetToken, newPassword), 3.seconds)

      if(passwordChanged){
        Future.successful(Redirect(routes.HomeController.loginPage))
      }
      else{
        Future.successful(Redirect(routes.UserController.passwordResetPage(passwordResetToken)).flashing("message" -> "There was a problem, please try again later."))
      }
    }
  }

  // TODO: figure out how to move this to a service component
  private def sendPasswordTokenResetLinkToEmail(passwordResetToken: String, emailTo: String) = {
    val passwordResetLink: String = s"http://localhost:9000/passwordReset?token=$passwordResetToken"
    println(s"Sending mail to $emailTo")
    val email = Email(
                  subject = "scalaProject - Password reset link",
                  from="scalaproject123@gmail.com",
                  to = Seq(emailTo), 
                  bodyText = Some(passwordResetLink))

       mailerClient.send(email)
  }
}
