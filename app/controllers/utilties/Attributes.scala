package controllers.utilties

import play.api.libs.typedmap.TypedKey

object Attributes {
  val UserID: TypedKey[Int] = TypedKey[Int]("userId")
}
