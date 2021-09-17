package models

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

abstract class DatabaseModel(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  def database:JdbcProfile#Backend#Database = db

  protected def notEmptyAndBiggerThanZero(integerOption: Option[Int]): Boolean = {
    integerOption.nonEmpty && integerOption.get > 0
  }

  protected def isBiggerThanZero(integerValue: Int): Boolean = {
    integerValue > 0
  }

  protected def notEmpty(option: Option[_]): Boolean = {
    option.nonEmpty
  }
}

