package controllers.utilties

import controllers.utilties.CredentialsValidityStates.CredentialsValidityStates

trait Checker {
  protected val credentialsValidityStatesMap: Map[CredentialsValidityStates.Value, String]

  protected def checkValidity(): CredentialsValidityStates

  def getValidityErrorMessage(): Option[String] = {
    var errorMessageResult: Option[String] = None
    val credentialsValidityState: CredentialsValidityStates = checkValidity()

    if (credentialsValidityState != CredentialsValidityStates.CredentialsValid) {
      errorMessageResult = Option(credentialsValidityStatesMap(credentialsValidityState))
    }

    errorMessageResult
  }
}