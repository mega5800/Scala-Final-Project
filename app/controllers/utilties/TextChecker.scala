package controllers.utilties

import controllers.utilties.CheckerTypes.CheckerTypes
import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

class TextChecker(checkerType: CheckerTypes, textValueToCheck: String = "", private var _minimalAmountOfChars: Int = 5)
  extends Checker(checkerType, textValueToCheck) {

  require(_minimalAmountOfChars > 0)
  require(_checkerType == CheckerTypes.Password || _checkerType == CheckerTypes.Username)

  def minimalAmountOfChars: Int = _minimalAmountOfChars

  def minimalAmountOfChars_=(newMinimalAmountOfChars: Int): Unit = {
    require(newMinimalAmountOfChars > 0)
    _minimalAmountOfChars = newMinimalAmountOfChars
  }

  override def checkerType_=(newCheckerType: CheckerTypes): Unit = {
    require(newCheckerType == CheckerTypes.Password || newCheckerType == CheckerTypes.Username)
    _checkerType = newCheckerType
  }

  override protected val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyText -> s"Please enter a ${_checkerType}",
      CredentialsValidityStates.TooShortText -> s"Please enter a ${_checkerType} with at least ${_minimalAmountOfChars} characters")

  override protected def checkValidity(): CredentialsValidityStates = {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState
    println(_textValueToCheck)
    if (_textValueToCheck.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyText
    else if (_textValueToCheck.length() < _minimalAmountOfChars) credentialsValidityState = CredentialsValidityStates.TooShortText
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    println(credentialsValidityState)
    credentialsValidityState
  }
}