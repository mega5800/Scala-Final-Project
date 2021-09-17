package controllers.utilties

object CredentialsValidityStates extends Enumeration {
  type CredentialsValidityStates = Value
  val EmptyState, EmptyUserName, TooShortUserName, EmptyPassword, TooShortPassword, EmptyEmail, InvalidEmail, CredentialsValid = Value
}
