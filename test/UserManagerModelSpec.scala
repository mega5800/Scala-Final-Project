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

class UserManagerModelSpec() extends DatabaseModelSpec {
  private val userManagerModel = new UserManagerModel(dbConfProvider)

  private var testUserId: Int = -1
  private val testUserUsername = "testUser"
  private val testUserPassword = "testPassword"
  private val testUserEmail = "test@mail.com"

  "UserManagerModel - user creation" must {
    "create new user with username and email that does not exist" in {
      noException should be thrownBy {
        testUserId = await(userManagerModel.createUser(testUserUsername, testUserPassword, testUserEmail))
      }

    }

    "new user must exist in database" in {
      await(userManagerModel.userExists(testUserUsername)) mustBe true
    }

    "new user must have the same values given when registered" in {
      val user = await(userManagerModel.getUserByUsername(testUserUsername)).get

      user.id mustBe testUserId
      BCrypt.checkpw(testUserPassword, user.password) mustBe true
      user.username mustBe testUserUsername
      user.email mustBe testUserEmail
      assert ((System.currentTimeMillis() - user.createdAt.get.getTime) <= 1000)
    }

    "fail upon creation of user with existing username" in {
      the[SQLException] thrownBy await(userManagerModel.createUser(testUserUsername, "pass", "test2@mail.com"))
    }

    "fail upon creation of user with existing email" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("testUser2", testUserPassword, testUserEmail))
    }

    "fail upon creation of user with existing email and username" in {
      the[SQLException] thrownBy await(userManagerModel.createUser(testUserUsername, testUserPassword, testUserEmail))
    }

    "fail upon creation of user with empty username field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser("", testUserPassword, testUserEmail))
    }

    "fail upon creation of user with empty email field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser(testUserUsername, testUserPassword, ""))
    }

    "fail upon creation of user with empty password field" in {
      the[SQLException] thrownBy await(userManagerModel.createUser(testUserUsername, "", testUserEmail))
    }

    // handled by the server not the database for now...
//    "fail upon creation of user with wrongly formatted email field" in {
//      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "badlyformattedemail"))
//      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "badly@fasf"))
//      the[SQLException] thrownBy await(userManagerModel.createUser("newUser", "newPassword", "222@gm,com"))
//    }
  }

  "userManagerModel - user validation" must {
    var sessionToken: Option[String] = Option.empty[String]
    "validate existing user with correct username and password" in {
      sessionToken = await(userManagerModel.validateUser(testUserUsername, testUserPassword))

      sessionToken must not equal None
    }

    "have the token exist for logged in user after validation" in {
      await(userManagerModel.userHasSessionToken(testUserId)) mustBe true
    }

    "have the user id of testUser connected to obtained session token during login" in {
      await(userManagerModel.getUserIdBySessionToken(sessionToken.get)) mustBe Some(testUserId)
    }

    "have the session token created for testUser connected to the user id of testUser" in {
      await(userManagerModel.getSessionTokenByUserId(testUserId)) mustBe sessionToken
    }

    "delete the session token that's connected to testUser successfully" in {
      await(userManagerModel.deleteUserSession(testUserId)) mustBe true
      await(userManagerModel.userHasSessionToken(testUserId)) mustBe false
      await(userManagerModel.getSessionTokenByUserId(testUserId)) mustBe None
      await(userManagerModel.getUserIdBySessionToken(sessionToken.get)) mustBe None
    }

    "reject user with username that does not exist" in {
      await(userManagerModel.validateUser("test", "admin")) mustBe None
    }

    "reject user with correct username and wrong password" in {
      await(userManagerModel.validateUser(testUserUsername, "wrongPassword")) mustBe None
    }

    "reject user incorrect username and password" in {
      await(userManagerModel.validateUser("someUser", "wrongPassword")) mustBe None
    }

    "reject user with empty username field" in {
      await(userManagerModel.validateUser("", "wrongPassword")) mustBe None
    }

    "reject user empty password field" in {
      await(userManagerModel.validateUser("someUser", "")) mustBe None
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
        passwordResetToken = await(userManagerModel.createPasswordResetToken(testUserEmail))
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
      cleanUp()
    }
  }

  override def cleanUp(): Unit = {
    await(userManagerModel.deleteUserByUsername(testUserUsername))
  }
}
