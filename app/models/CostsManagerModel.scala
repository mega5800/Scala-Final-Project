package models

import models.Tables.{UserCosts, UserCostsRow, Users}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CostsManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  def getCostsForUser(userId: Int): Future[Seq[UserCostsRow]] = {
    val getCostsForUserQuery = UserCosts.filter(_.userId === userId)

    database.run(getCostsForUserQuery.result)
  }

  def getCostForUser(userId: Int, costId: Int): Future[Option[UserCostsRow]] = {
      val getCostForUserAction = UserCosts.filter(cost => cost.userId === userId && cost.costId === costId).result.headOption

      database.run(getCostForUserAction)
  }

  def addSingleCostForUser(costToAdd: UserCostsRow): Future[Boolean] = {
    val addSingleCostForUserAction = UserCosts += costToAdd

    database.run(addSingleCostForUserAction).map(isBiggerThanZero)
  }

  def updateCostForUser(updatedCost: UserCostsRow): Future[Boolean] = {
    database.run(UserCosts.update(updatedCost)).map(isBiggerThanZero)
  }

  def deleteCostForUser(userId: Int, costId: Int): Future[Boolean] = {
    val deleteCostForUserAction = UserCosts.filter(cost => cost.userId === userId && cost.costId === costId).delete

    database.run(deleteCostForUserAction).map(isBiggerThanZero)
  }

  def deleteAllCostsForUser(userId: Int): Future[Boolean] = {
    val deleteAllCostForUser = UserCosts.filter(_.userId === userId).delete

    database.run(deleteAllCostForUser).map(isBiggerThanZero)
  }

  def addCostsForUser(costsToAdd: Seq[UserCostsRow]): Future[Boolean] = {
    val addCostsForUserAction = UserCosts ++= costsToAdd

    database.run(addCostsForUserAction).map(insertCount => {
      println("Bulk add count(?): " + insertCount.get)
      insertCount.get == costsToAdd.length
    })
  }
}