package controllers.utilties

import controllers.utilties.CheckerTypes.CheckerTypes
import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

class TextChecker(private var checkerType:CheckerTypes, private var textValueToCheck:String="", private var minimalAmountOfChars:Int = 5)
{
  require(minimalAmountOfChars > 0)
  require(checkerType == CheckerTypes.Password || checkerType == CheckerTypes.Username)

  private val credentialsValidityStatesMap =
    Map(CredentialsValidityStates.EmptyText->s"Please enter a $checkerType",
        CredentialsValidityStates.TooShortText->s"Please enter a $checkerType with at least $minimalAmountOfChars characters")

  def CheckerType:CheckerTypes = checkerType
  def CheckerType_=(newCheckerType:CheckerTypes): Unit =
  {
    require(newCheckerType == CheckerTypes.Password || newCheckerType == CheckerTypes.Username)
    checkerType = newCheckerType
  }

  def TextValueToCheck:String = textValueToCheck
  def TextValueToCheck_=(newTextValueToCheck:String) {textValueToCheck = newTextValueToCheck}

  def MinimalAmountOfChars:Int = minimalAmountOfChars
  def MinimalAmountOfChars_(newMinimalAmountOfChars:Int): Unit =
  {
    require(newMinimalAmountOfChars > 0)
    minimalAmountOfChars = newMinimalAmountOfChars
  }

  private def checkGivenTextValidity():CredentialsValidityStates =
  {
    var credentialsValidityState: CredentialsValidityStates = CredentialsValidityStates.EmptyState

    if (textValueToCheck.isEmpty) credentialsValidityState = CredentialsValidityStates.EmptyText
    else if (textValueToCheck.length() < minimalAmountOfChars) credentialsValidityState = CredentialsValidityStates.TooShortText
    else credentialsValidityState = CredentialsValidityStates.CredentialsValid

    credentialsValidityState
  }

  def getGivenTextValidityErrorMessage():Option[String] =
  {
    var errorMessageResult: Option[String] = None
    val credentialsValidityState: CredentialsValidityStates = checkGivenTextValidity()

    if (credentialsValidityState != CredentialsValidityStates.CredentialsValid)
    {
      errorMessageResult = Option(credentialsValidityStatesMap(credentialsValidityState))
    }

    errorMessageResult
  }
}