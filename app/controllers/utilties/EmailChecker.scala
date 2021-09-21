package controllers.utilties

import controllers.utilties
import controllers.utilties.CheckerTypes.CheckerTypes
import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

class EmailChecker(private var checkerType: CheckerTypes, private var emailValueToCheck: String = "") extends Checker {
  require(checkerType == CheckerTypes.Email)

  override protected val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyEmail -> "Please enter an email",
      CredentialsValidityStates.InvalidEmail -> "Please enter a valid email")

  private val emailRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".r

  def CheckerType: CheckerTypes = checkerType

  def CheckerType_=(newCheckerType: CheckerTypes): Unit = {
    require(newCheckerType == CheckerTypes.Email)
    checkerType = newCheckerType
  }

  def TextValueToCheck: String = emailValueToCheck

  def TextValueToCheck_=(newTextValueToCheck: String) {
    emailValueToCheck = newTextValueToCheck
  }

  override protected def checkValidity(): CredentialsValidityStates = {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (emailValueToCheck.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyEmail
    else if (!emailValidityCheck(emailValueToCheck)) credentialsValidityState = CredentialsValidityStates.InvalidEmail
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    //inner function for checking email validity
    def emailValidityCheck(email: String): Boolean = email match {
      case null => false
      case email if email.trim.isEmpty => false
      case email if emailRegex.findFirstMatchIn(email).isDefined => true
      case _ => false
    }

    credentialsValidityState
  }
}