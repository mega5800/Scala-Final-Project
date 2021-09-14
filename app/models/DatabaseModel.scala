package models

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

abstract class DatabaseModel(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

//  protected val queryTimeout: FiniteDuration = 3.seconds
  def database:JdbcProfile#Backend#Database = db
//  private val defaultRecovery: PartialFunction[Throwable, Option[Nothing]] = {
//    case error =>
//      println(error.getMessage)
//      None
//  }

//  protected def getResultWithRecovery[T](action: DBIOAction[T, NoStream, Nothing])(implicit onRecovery: PartialFunction[Throwable, Option[T]] = defaultRecovery): Future[Option[T]] = {
//    database.run(action).map { value =>
//      Some(value)
//    }.recover(onRecovery)
//  }
//
//  protected def getCollectionAsTry[T](action: DBIOAction[Seq[T], NoStream, Nothing])(implicit onRecovery: PartialFunction[Throwable, Option[Seq[T]]] = defaultRecovery): Future[Option[Seq[T]]] = {
//    database.run(action).map { collection =>
//      Some(collection)
//    }.recover(onRecovery)
//  }
//
//  protected def getHeadOfCollectionWithRecovery[T](action: DBIOAction[Seq[T], NoStream, Nothing])(implicit onRecovery: PartialFunction[Throwable, Option[T]] = defaultRecovery): Future[Option[T]] = {
//    database.run(action).map { collection =>
//        collection.headOption
//    }.recover(onRecovery)
//  }

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

