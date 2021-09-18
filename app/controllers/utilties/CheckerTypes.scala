package controllers.utilties

object CheckerTypes extends Enumeration {
  type CheckerTypes = String
  val Email = "email"
  val Username = "username"
  val Password = "password"
}