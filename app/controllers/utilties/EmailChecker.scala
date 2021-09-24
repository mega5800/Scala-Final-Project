package controllers.utilties

import controllers.utilties.CheckerTypes.CheckerTypes
import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

class EmailChecker(checkerType: CheckerTypes, emailValueToCheck: String = "")
  extends Checker(checkerType, emailValueToCheck) {

  require(_checkerType == CheckerTypes.Email)

  private val emailRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""".r

  override def checkerType_=(newCheckerType: CheckerTypes): Unit = {
    require(newCheckerType == CheckerTypes.Email)
    _checkerType = newCheckerType
  }

  override protected val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyEmail -> "Please enter an email",
      CredentialsValidityStates.InvalidEmail -> "Please enter a valid email")

  override protected def checkValidity(): CredentialsValidityStates = {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (_textValueToCheck.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyEmail
    else if (!emailValidityCheck(_textValueToCheck)) credentialsValidityState = CredentialsValidityStates.InvalidEmail
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