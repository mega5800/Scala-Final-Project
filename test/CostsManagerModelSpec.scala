import models.CostsManagerModel
import models.Tables.UserCostsRow

import java.sql.Timestamp

class CostsManagerModelSpec extends DatabaseModelSpec {
  private val costsManagerModel = new CostsManagerModel(dbConfProvider)

  // assume there are currently two test users in the database
  private val testUserId1 = 1;
  private val testUserId2 = 2;

  "CostsManagerModel" must {
    "add a single item for testUser1" in {
      val costToAdd = UserCostsRow(-1, testUserId2, "Pizza", new Timestamp(System.currentTimeMillis()), "Food", 50.0)
      costsManagerModel.addSingleCostForUser(costToAdd) mustBe true
    }
  }

  override def cleanUp(): Unit = ???
}
