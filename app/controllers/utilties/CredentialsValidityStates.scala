package controllers.utilties

object CredentialsValidityStates extends Enumeration {
  type CredentialsValidityStates = Value
  val EmptyState, EmptyText, TooShortText, EmptyEmail, InvalidEmail, CredentialsValid = Value
}
