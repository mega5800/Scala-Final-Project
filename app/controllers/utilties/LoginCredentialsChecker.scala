package controllers.utilties

import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

class LoginCredentialsChecker(private var userName:String="", private var password:String="")
{
  private val minimumLengthOfUserName: Int = 5
  private val minimumLengthOfPassword: Int = 5

  private val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyUserName->"Please enter an username",
        CredentialsValidityStates.TooShortUserName->s"Please enter a username with at least $minimumLengthOfUserName characters",
        CredentialsValidityStates.EmptyPassword->"Please enter a password",
        CredentialsValidityStates.TooShortPassword->s"Please enter a password with at least $minimumLengthOfPassword characters")

  def UserName:String = userName
  def UserName_=(newUserName:String) {userName = newUserName}

  def Password:String = password
  def Password_=(newPassword:String) {password = newPassword}

  private def checkLoginCredentialsValidity():CredentialsValidityStates =
  {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (userName.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyUserName
    else if (userName.length() < minimumLengthOfUserName) credentialsValidityState = CredentialsValidityStates.TooShortUserName
    else if (password.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyPassword
    else if (password.length() < minimumLengthOfPassword) credentialsValidityState = CredentialsValidityStates.TooShortPassword
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    credentialsValidityState
  }

  def getLoginCredentialsValidityErrorMessage():Option[String] =
  {
    var errorMessageResult: Option[String] = None
    val credentialsValidityState: CredentialsValidityStates = checkLoginCredentialsValidity()

    if (credentialsValidityState != CredentialsValidityStates.CredentialsValid)
    {
      errorMessageResult = Option(credentialsValidityStatesMap(credentialsValidityState))
    }

    errorMessageResult
  }
}