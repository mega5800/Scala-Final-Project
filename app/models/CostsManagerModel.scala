package models

import models.Tables.{Costs, CostsRow, Users}
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

import java.sql.SQLException
import scala.async.Async.{async, await}


class CostsManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider, userManagerModel: UserManagerModel)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  def getCostsForUser(userId: Int): Future[Seq[CostsRow]] = {
    val getCostsForUserQuery = Costs.filter(_.userId === userId)

    database.run(getCostsForUserQuery.result)
  }

  def getCostForUser(userId: Int, costId: Int): Future[CostsRow] = ???

  def addSingleCostForUser(userId: Int, costToAdd: CostsRow): Future[Boolean] = ???

  def updateCostForUser(userId: Int, updatedCost: CostsRow): Future[Boolean] = ???

  def deleteCostForUser(userId: Int, costId: Int): Future[Boolean] = ???

  def deleteAllCostsForUser(userId: Int): Future[Boolean] = ???

  def addCostsForUser(userId: Int, costsToAdd: Seq[CostsRow]): Future[Boolean] = ???
}