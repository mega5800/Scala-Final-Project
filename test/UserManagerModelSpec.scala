import models._
import org.mindrot.jbcrypt.BCrypt
import org.scalatest.time.Span
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

import java.sql
import java.sql.SQLException
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

class UserManagerModelSpec() extends PlaySpec {
  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  lazy val dbConfProvider: DatabaseConfigProvider = injector.instanceOf[DatabaseConfigProvider]
  lazy val executionContext: ExecutionContext = injector.instanceOf[ExecutionContext]
  private val queryTime: Span = 2.seconds

  val userManagerModel = new UserManagerModel(dbConfProvider)(executionContext)

  "UserManagerModel - user creation" must {
    var userId: Int = -1
    "create new user with username and email that does not exist" in {
      noException should be thrownBy {
        userId = await(userManagerModel.createUser("testUser", "testPassword", "test@mail.com"))
      }

    }

    "new user must exist in database" in {
      await(userManagerModel.userExists("testUser")) mustBe true
    }

    "new user must have the same values given when registered" in {
      val user = await(userManagerModel.getUserByUsername("testUser")).get

      user.id mustBe userId
      BCrypt.checkpw("testPassword", user.password) mustBe true
      user.username mustBe "testUser"
      user.email mustBe "test@mail.com"
    }

    "fail upon creation of user with existing username" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser", "pass", "test2@mail.com"))
    }

    "fail upon creation of user with existing email" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser2", "testPassword", "test@mail.com"))
    }

    "fail upon creation of user with existing email and username" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser", "testPassword", "test@mail.com"))
    }

    "fail upon creation of user with empty username field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("", "testPassword", "test@mail.com"))
    }

    "fail upon creation of user with empty email field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser", "testPassword", ""))
    }

    "fail upon creation of user with empty password field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser", "", "test@mail.com"))
    }

    "fail upon creation of user with wrongly formatted email field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "badlyformattedemail"))
      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "badly@fasf"))
      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "222@gm,com"))
    }
  }

  "userManagerModel - user validation" must {
    "validate existing user with correct username and password" in {
      await(userManagerModel.validateUser("testUser", "testPassword")) mustBe true
    }

    "reject user with username that does not exist" in {
      await(userManagerModel.validateUser("test", "admin")) mustBe false
    }

    "reject user with correct username and wrong password" in {
      await(userManagerModel.validateUser("testUser", "wrongPassword")) mustBe false
    }

    "reject user incorrect username and password" in {
      await(userManagerModel.validateUser("someUser", "wrongPassword")) mustBe false
    }

    "reject user with empty username field" in {
      await(userManagerModel.validateUser("", "wrongPassword")) mustBe false
    }

    "reject user empty password field" in {
      await(userManagerModel.validateUser("someUser", "")) mustBe false
    }
  }

  "UserManagerModel - user removal" must {
    "remove users that exists in the database" in {
      var user1Id = -1
      noException should be thrownBy {
        user1Id = await(userManagerModel.createUser("user1", "password1", "user1@email.com"))
        await(userManagerModel.createUser("user2", "password2", "user2@email.com"))
        await(userManagerModel.createUser("user3", "password3", "user3@email.com"))
      }

      await(userManagerModel.getUserById(user1Id)).get.username mustBe "user1"
      await(userManagerModel.getUserByUsername("user2")).get.username mustBe "user2"
      await(userManagerModel.getUserByEmail("user3@email.com")).get.username mustBe "user3"

      await(userManagerModel.deleteUserById(user1Id)) mustBe true
      await(userManagerModel.deleteUserByUsername("user2")) mustBe true
      await(userManagerModel.deleteUserByEmail("user3@email.com")) mustBe true

      await(userManagerModel.getUserById(user1Id)) mustBe None
      await(userManagerModel.getUserByUsername("user2")) mustBe None
      await(userManagerModel.getUserByEmail("user3@email.com")) mustBe None
    }

    "fail deleting upon id that doesn't exist" in {
      await(userManagerModel.deleteUserById(-5)) mustBe false
      await(userManagerModel.deleteUserById(0)) mustBe false
    }

    "fail deleting upon username that doesn't exist" in {
      await(userManagerModel.deleteUserByUsername("idontexist")) mustBe false
      await(userManagerModel.deleteUserByUsername("user1")) mustBe false
      await(userManagerModel.deleteUserByUsername("user2")) mustBe false
    }

    "fail deleting upon email that doesn't exist" in {
      await(userManagerModel.deleteUserByEmail("idontexist@email.com")) mustBe false
      await(userManagerModel.deleteUserByEmail("user1@email.com")) mustBe false
      await(userManagerModel.deleteUserByEmail("user2@email.com")) mustBe false
    }

    "fail deleting upon empty username field" in {
      await(userManagerModel.deleteUserByUsername("")) mustBe false
    }

    "fail deleting upon empty email field" in {
      await(userManagerModel.deleteUserByEmail("")) mustBe false
    }
  }

  "UserManagerModel - password reset" must {
    var passwordResetToken: String = ""
    "create password token for email that exists in the database" in {
      noException should be thrownBy {
        passwordResetToken = await(userManagerModel.createPasswordResetToken("test@mail.com"))
      }
    }

    "fail to create password token for email that does not exist in the database" in {
      the[SQLException] thrownBy {
        await(userManagerModel.createPasswordResetToken("doesntexit@mail.com"))
      } must have message "[createPasswordResetToken]: Could not find user with email doesntexit@mail.com"
    }

    "confirm validity of token that hasn't expired in the database" in {
      await(userManagerModel.isPasswordResetTokenValid(passwordResetToken)) mustBe true
    }

    "fail validating token upon token that does not exist in the database" in {
      await(userManagerModel.isPasswordResetTokenValid("idontexxisttt")) mustBe false
    }

    "fail validating token upon empty token field" in {
      await(userManagerModel.isPasswordResetTokenValid("")) mustBe false
    }

    "fail to reset password upon invalid token" in {
      the[SQLException] thrownBy {
        await(userManagerModel.updatePasswordWithPasswordResetToken("invalidtoken", "newPassword"))
      } must have message "[updatePasswordWithPasswordResetToken]: passwordResetToken invalidtoken is invalid"
    }

    "fail to reset password upon empty token field" in {
      the[SQLException] thrownBy {
        await(userManagerModel.updatePasswordWithPasswordResetToken("", "newPassword"))
      } must have message "[updatePasswordWithPasswordResetToken]: passwordResetToken  is invalid"
    }

    "fail to reset password upon empty password field but valid token" in {
      the[SQLException] thrownBy {
        await(userManagerModel.updatePasswordWithPasswordResetToken(passwordResetToken, ""))
      } must have message "[updatePasswordWithPasswordResetToken]: empty password field"
    }

    "reset password for a valid token" in {
      await(userManagerModel.updatePasswordWithPasswordResetToken(passwordResetToken, "newPassword")) mustBe true
    }
  }


  "UserManagerModel - test clean up" must {
    "delete created users from database" in {
      await(cleanUp()) mustBe true
    }
  }

  private def await[DataType](awaitable: Awaitable[DataType]): DataType = {
    Await.result(awaitable, queryTime)
  }

  private def cleanUp[T](): Future[Boolean] = {
    userManagerModel.deleteUserByUsername("testUser")
  }
}
