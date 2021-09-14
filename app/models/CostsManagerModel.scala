package models

import models.Tables.{Costs, CostsRow, Users}
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

import java.sql.SQLException
import scala.async.Async.{async, await}


class CostsManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider, userManagerModel: UserManagerModel)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  def getCostsForUser(username: String): Future[Seq[CostsRow]] = async {
    val userOption = await(userManagerModel.getUserByUsername(username))

    userOption match {
      case Some(foundUser) =>
        val getCostsForUserQuery = for {
          (user, costs) <- Users join Costs on (_.id === _.userId)
          if user.username === foundUser.username
        } yield costs

        await(database.run(getCostsForUserQuery.result))
      case None => throw new SQLException(s"[getCostsForUser]: user ${username} does not exist")
    }
  }
}