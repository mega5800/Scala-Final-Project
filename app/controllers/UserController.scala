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
}
