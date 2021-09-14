package controllers

import controllers.CredentialsValidityStates.CredentialsValidityStates
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

class RegisterCredentialsChecker(private var userName:String="", private var password:String="", private var email:String="")
{
  private val loginCredentialsChecker: LoginCredentialsChecker = new LoginCredentialsChecker()

  private val emailRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".r

  private val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyEmail->"Please enter an email",
        CredentialsValidityStates.InvalidEmail->"Please enter a valid email")

  def Email:String = email
  def Email_=(newEmail:String) {email = newEmail}

  def UserName:String = loginCredentialsChecker.UserName
  def UserName_=(newUserName:String) {loginCredentialsChecker.UserName = newUserName}

  def Password:String = loginCredentialsChecker.Password
  def Password_=(newPassword:String) {loginCredentialsChecker.Password = newPassword}

  def getRegisterCredentialsValidityResult(suitableWebPage:Call):Result =
  {
    var result:Result = loginCredentialsChecker.getLoginCredentialsValidityResult(suitableWebPage)

    if (result == null)
    {
      val credentialsValidityState = checkRegisterCredentialsValidity()
      if (credentialsValidityState != CredentialsValidityStates.CredentialsValid)
      {
        result = Redirect(suitableWebPage).flashing("error" -> credentialsValidityStatesMap(credentialsValidityState))
      }
    }

    result
  }

  private def checkRegisterCredentialsValidity():CredentialsValidityStates =
  {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (email.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyEmail
    else if (!emailValidityCheck(email)) credentialsValidityState = CredentialsValidityStates.InvalidEmail
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    //inner function for checking email validity
    def emailValidityCheck(email: String): Boolean = email match
    {
      case null                                                   => false
      case email if email.trim.isEmpty                            => false
      case email if emailRegex.findFirstMatchIn(email).isDefined  => true
      case _                                                      => false
    }

    credentialsValidityState
  }
}