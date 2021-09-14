package controllers

import controllers.CredentialsValidityStates.CredentialsValidityStates
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

  // TODO: Create an object that checks the input validity
  private val minimumLengthOfUserName: Int = 5
  private val minimumLengthOfPassword: Int = 5

  private val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyUserName->"Please enter an username",
        CredentialsValidityStates.TooShortUserName->s"Please enter a username with at least $minimumLengthOfUserName characters",
        CredentialsValidityStates.EmptyPassword->"Please enter a password",
        CredentialsValidityStates.TooShortPassword->s"Please enter a password with at least $minimumLengthOfPassword characters",
        CredentialsValidityStates.EmptyEmail->"Please enter an email",
        CredentialsValidityStates.InvalidEmail->"Please enter a valid email")

  //TODO: check new regex expression
  private val emailRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".r

  //TODO: merge 2 functions into one!
  def checkCredentialsValidity(userName:String, password:String, email:String) : CredentialsValidityStates =
  {
      var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

      if (userName.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyUserName
      else if (userName.length() < minimumLengthOfUserName) credentialsValidityState = CredentialsValidityStates.TooShortUserName
      else if (password.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyPassword
      else if (password.length() < minimumLengthOfPassword) credentialsValidityState = CredentialsValidityStates.TooShortPassword
      else if (email.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyEmail
      else if (!emailValidityCheck(email)) credentialsValidityState = CredentialsValidityStates.InvalidEmail
      else credentialsValidityState = CredentialsValidityStates.CredentialsValid

      //inner function for checking email validity
      def emailValidityCheck(e: String): Boolean = e match
      {
        case null                                           => false
        case e if e.trim.isEmpty                            => false
        case e if emailRegex.findFirstMatchIn(e).isDefined  => true
        case _                                              => false
      }

      credentialsValidityState
  }

  def checkCredentialsValidity(userName:String, password:String) : CredentialsValidityStates =
  {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (userName.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyUserName
    else if (userName.length() < minimumLengthOfUserName) credentialsValidityState = CredentialsValidityStates.TooShortUserName
    else if (password.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyPassword
    else if (password.length() < minimumLengthOfPassword) credentialsValidityState = CredentialsValidityStates.TooShortPassword
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    credentialsValidityState
  }

  def createUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    val username = credentials("username").head
    val password = credentials("password").head
    val email = credentials("email").head

    val credentialsValidityState: CredentialsValidityStates = checkCredentialsValidity(username, password, email)

    var result: Result = null
    if (credentialsValidityState == CredentialsValidityStates.CredentialsValid)
    {
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
    else
    {
      result = Redirect(routes.HomeController.registerPage).flashing("error" -> credentialsValidityStatesMap(credentialsValidityState))
    }

    Future(result)
  }

  def validateUser: Action[AnyContent] = Action.async { request =>
    val credentials = request.body.asFormUrlEncoded.get // never empty
    val username = credentials("username").head
    val password = credentials("password").head

    // AVITAL: assume it is empty
    var result: Result = null
    val credentialsValidityState: CredentialsValidityStates = checkCredentialsValidity(username, password)
    if (credentialsValidityState == CredentialsValidityStates.CredentialsValid)
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
    else
    {
      result = Redirect(routes.HomeController.loginPage).flashing("error" -> credentialsValidityStatesMap(credentialsValidityState))
    }

    Future(result)
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.HomeController.loginPage).withNewSession
   }
}
