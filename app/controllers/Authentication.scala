package controllers

sealed abstract class Authentication

final case class AuthenticationSuccess(authenticationData: String) extends Authentication

final case class AuthenticationFailure() extends Authentication

