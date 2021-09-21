import models.{CostsManagerModel, UserManagerModel}
import models.Tables.UserItemCostsRow

import java.sql.{SQLException, Timestamp}

class CostsManagerModelSpec extends DatabaseModelSpec {
  private val costsManagerModel = new CostsManagerModel(dbConfProvider)
  private val userManagerModel = new UserManagerModel(dbConfProvider)

  val currentTimestamp = Some(new Timestamp(System.currentTimeMillis()))
  private val testUserId1 = await(userManagerModel.createUser("testUser1", "testPassword1", "testUser1@gmail.com"));
  private val testUserId2 = await(userManagerModel.createUser("testUser2", "testPassword2", "testUser2@gmail.com"));;
  private val singleCostToAdd = UserItemCostsRow(-1, testUserId1, "Pizza", currentTimestamp, "Food", 50.0)
  private val costsCollection = List(
    UserItemCostsRow(-1, testUserId2, "Phone charger", currentTimestamp, "Tech", 125),
    UserItemCostsRow(-1, testUserId2, "Monitor", currentTimestamp, "Tech", 830),
    UserItemCostsRow(-1, testUserId2, "Hamburger", currentTimestamp, "Food", 45),
    UserItemCostsRow(-1, testUserId2, "Control", currentTimestamp, "Video games", 250),
    UserItemCostsRow(-1, testUserId2, "Mass Effect Legendary Edition", currentTimestamp, "Video games", 400),
    UserItemCostsRow(-1, testUserId2, "Black T-Shirt", currentTimestamp, "Clothing", 75)
  )

  "CostsManagerModel" must {
    "add a single cost for testUser1" in {
      await(costsManagerModel.addSingleCostForUser(singleCostToAdd)) mustBe 1
    }

    "get exception when trying to add a single cost for a user that does not exist" in {
      the[SQLException] thrownBy {
        await(costsManagerModel.addSingleCostForUser(UserItemCostsRow(-1, 5, "Sandwich", currentTimestamp, "Food", 15)))
      }
    }

    "add a collection of costs for testUser2" in {
      await(costsManagerModel.addCostsForUser(costsCollection)) mustBe true
    }

    "get a single cost for testUsers" in {
      var cost = await(costsManagerModel.getSingleCostForUser(testUserId1, 1))
      cost.nonEmpty mustBe true
      compareCostsWithoutCostId(cost.get, singleCostToAdd) mustBe true

      cost = await(costsManagerModel.getSingleCostForUser(testUserId2, 4) )
      cost.nonEmpty mustBe true
      compareCostsWithoutCostId(cost.get, costsCollection(3)) mustBe true
    }

    "get all costs for testUsers" in {
      var costs = await(costsManagerModel.getAllCostsForUser(testUserId1))
      compareCostSequencesWithoutCostId(costs, List(singleCostToAdd)) mustBe true

      costs = await(costsManagerModel.getAllCostsForUser(testUserId2))
      compareCostSequencesWithoutCostId(costs, costsCollection) mustBe true
    }

    "delete a single cost" in {
      await(costsManagerModel.deleteCostForUser(testUserId2, 2)) mustBe true
    }

    "get None when getting a cost that does not exist" in {
      await(costsManagerModel.getSingleCostForUser(testUserId2, 2)) mustBe None
    }

    "get None when getting a cost for a user that does not exist" in {
      await(costsManagerModel.getSingleCostForUser(5, 1)) mustBe None
    }

    "get an empty list when getting collection of costs for a user that does not exist" in {
      await(costsManagerModel.getAllCostsForUser(5)) mustBe Seq.empty
    }

    "fail upon trying to delete all costs for a user that does not exist" in {
      await(costsManagerModel.deleteAllCostsForUser(5)) mustBe false
    }

    "update cost for testUser2" in {
      val oldCost = costsCollection(0)
      val updatedCost = UserItemCostsRow(1, testUserId2, "Phone charger and cable", oldCost.purchaseDate, oldCost.category, oldCost.itemPrice)
      await(costsManagerModel.updateCostForUser(updatedCost)) mustBe true

      val cost = await(costsManagerModel.getSingleCostForUser(testUserId2, 1))
      cost.nonEmpty mustBe true
      cost.get.itemName mustBe "Phone charger and cable"
    }

    "delete all costs for testUsers" in {
      await(costsManagerModel.deleteAllCostsForUser(testUserId1)) mustBe true
      await(costsManagerModel.getAllCostsForUser(testUserId1)) mustBe Seq.empty

      await(costsManagerModel.deleteAllCostsForUser(testUserId2)) mustBe true
      await(costsManagerModel.getAllCostsForUser(testUserId2)) mustBe Seq.empty
    }

    "cleanup" in {
      cleanUp()
    }
  }

  def compareCostsWithoutCostId(firstCost: UserItemCostsRow, secondCost: UserItemCostsRow): Boolean = {
        firstCost.userId == secondCost.userId &&
        firstCost.itemName == secondCost.itemName &&
        firstCost.purchaseDate == secondCost.purchaseDate &&
        firstCost.category == secondCost.category &&
        firstCost.itemPrice == secondCost.itemPrice
  }

  def compareCostSequencesWithoutCostId(firstCostList: Seq[UserItemCostsRow], secondCostList: Seq[UserItemCostsRow]): Boolean = {
    if(firstCostList.length != secondCostList.length) return false

    var isEqual = true

    for(i <- firstCostList.indices){
      if(!compareCostsWithoutCostId(firstCostList(i), secondCostList(i))){
        isEqual = false
      }
    }

    isEqual
  }

  override def cleanUp(): Unit = {
    await(userManagerModel.deleteUserById(testUserId1))
    await(userManagerModel.deleteUserById(testUserId2))
  }
}
