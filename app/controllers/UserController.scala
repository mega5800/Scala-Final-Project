package controllers

import models.UserManagerModel
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class UserController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
 extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile]{
  private val userManagerModel: UserManagerModel = new UserManagerModel(db)

  private val loginCredentialsChecker:LoginCredentialsChecker = new LoginCredentialsChecker()
  private val registerCredentialsChecker:RegisterCredentialsChecker = new RegisterCredentialsChecker()

  def createUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    registerCredentialsChecker.UserName = credentials("username").head
    registerCredentialsChecker.Password = credentials("password").head
    registerCredentialsChecker.Email = credentials("email").head

    var result: Result = registerCredentialsChecker.getRegisterCredentialsValidityResult(routes.HomeController.registerPage)
    if (result == null)
    {
      val userCreated = Await.result(userManagerModel.createUser(registerCredentialsChecker.UserName, registerCredentialsChecker.Password, registerCredentialsChecker.Email), 5.seconds)

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

  def validateUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    loginCredentialsChecker.UserName = credentials("username").head
    loginCredentialsChecker.Password = credentials("password").head

    var result: Result = loginCredentialsChecker.getLoginCredentialsValidityResult(routes.HomeController.loginPage)
    if (result == null)
    {
      val userValidated = Await.result(userManagerModel.validateUser(loginCredentialsChecker.UserName, loginCredentialsChecker.Password), 5.seconds)

      if(userValidated)
      {
        result = Redirect(routes.HomeController.index).withSession("username" -> loginCredentialsChecker.UserName)
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
}
