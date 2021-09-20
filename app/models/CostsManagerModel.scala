package models

import models.Tables.{UserItemCosts, UserItemCostsRow}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CostsManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  def getAllCostsForUser(userId: Int): Future[Seq[UserItemCostsRow]] = {
    val getCostsForUserQuery = UserItemCosts.sortBy(_.itemId).filter(_.userId === userId)

    database.run(getCostsForUserQuery.result)
  }

  def getSingleCostForUser(userId: Int, itemId: Int): Future[Option[UserItemCostsRow]] = {
      val getCostForUserAction = UserItemCosts.filter(cost => cost.userId === userId && cost.itemId === itemId).result.headOption

      database.run(getCostForUserAction)
  }

  def addSingleCostForUser(costToAdd: UserItemCostsRow): Future[Int] = {
    val addSingleCostForUserAction = (UserItemCosts returning UserItemCosts.map(_.itemId)) += costToAdd

    database.run(addSingleCostForUserAction)
  }

  def updateCostForUser(updatedCost: UserItemCostsRow): Future[Boolean] = {
    val updateCostForUserQuery = for {
      cost <- UserItemCosts if cost.userId === updatedCost.userId && cost.itemId === updatedCost.itemId
    } yield (cost.itemName, cost.purchaseDate, cost.category, cost.itemPrice)

    val updateCostForUserAction = updateCostForUserQuery.update((updatedCost.itemName, updatedCost.purchaseDate, updatedCost.category, updatedCost.itemPrice))

    database.run(updateCostForUserAction).map(isBiggerThanZero)
  }

  def deleteCostForUser(userId: Int, itemId: Int): Future[Boolean] = {
    val deleteCostForUserAction = UserItemCosts.filter(cost => cost.userId === userId && cost.itemId === itemId).delete

    database.run(deleteCostForUserAction).map(isBiggerThanZero)
  }

  def deleteAllCostsForUser(userId: Int): Future[Boolean] = {
    val deleteAllCostForUser = UserItemCosts.filter(_.userId === userId).delete

    database.run(deleteAllCostForUser).map(isBiggerThanZero)
  }

  def addCostsForUser(costsToAdd: Seq[UserItemCostsRow]): Future[Boolean] = {
    val addCostsForUserAction = UserItemCosts ++= costsToAdd

    database.run(addCostsForUserAction).map(insertCount => {
      insertCount.get == costsToAdd.length
    })
  }
}