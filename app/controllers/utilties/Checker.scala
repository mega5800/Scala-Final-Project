package controllers.utilties

import controllers.utilties.CheckerTypes.CheckerTypes
import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

abstract class Checker(protected var _checkerType: CheckerTypes, protected var _textValueToCheck: String) {
  // default methods

  def textValueToCheck: String = _textValueToCheck

  def textValueToCheck_=(newTextValueToCheck: String): Unit = {
    _textValueToCheck = newTextValueToCheck
  }

  def checkerType: CheckerTypes = _checkerType

  def getValidityErrorMessage(): Option[String] = {
    var errorMessageResult: Option[String] = None
    val credentialsValidityState: CredentialsValidityStates = checkValidity()

    if (credentialsValidityState != CredentialsValidityStates.CredentialsValid) {
      errorMessageResult = Option(credentialsValidityStatesMap(credentialsValidityState))
    }

    errorMessageResult
  }

  // abstract methods / fields
  protected val credentialsValidityStatesMap: Map[CredentialsValidityStates.Value, String]

  def checkerType_=(newCheckerType: CheckerTypes): Unit

  protected def checkValidity(): CredentialsValidityStates
}