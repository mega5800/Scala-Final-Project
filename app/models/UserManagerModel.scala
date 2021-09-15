package models

import models.Tables._
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import java.security.SecureRandom
import java.sql.{SQLException, Timestamp}
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class UserManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  private val secureRandom: SecureRandom = new SecureRandom()
  private val base64Encoder: Base64.Encoder = Base64.getUrlEncoder
  private val passwordResetTokenBitSize = 88
  private val userSessionTokenBitSize = 512
  private val passwordRequestExpirationDurationMinutes = 30

  def validateUser(username: String, password: String): Future[Option[String]] = async {
    val userOption = await(getUserByUsername(username))
    var sessionTokenResult: Option[String] = Option.empty[String]

    userOption match {
      case Some(user) =>
        val isPasswordValid = BCrypt.checkpw(password, user.password)

        if (isPasswordValid) {
          sessionTokenResult = await(getSessionTokenByUserId(user.id))

          if(sessionTokenResult.isEmpty){
            val newUserSessionToken = generateRandomToken(userSessionTokenBitSize)
            val insertSessionTokenAction = UserSessions += UserSessionsRow(-1, user.id, newUserSessionToken)
            val sessionTokenCreated = await(database.run(insertSessionTokenAction).map(isBiggerThanZero))

            if (sessionTokenCreated)
              sessionTokenResult = Some(newUserSessionToken)
          }
        }

        sessionTokenResult
      case None => None
    }
  }

  def createUser(username: String, password: String, email: String): Future[Int] = {
    // encrypt the password with default salt given by BCrypt library
    val encryptedPassword: String = BCrypt.hashpw(password, BCrypt.gensalt())

    // passing negative 1 to automatically generate an id on the database
    val currentTimestamp = Some(new Timestamp(System.currentTimeMillis()))
    val userToAdd: UsersRow = UsersRow(-1, username, encryptedPassword, email, currentTimestamp)
    val addUserQuery = (Users returning Users.map(_.id)) += userToAdd // or use Users.insertOrUpdate(userToAdd)

    // run the query by the database
    database.run(addUserQuery)
  }

  def userHasSessionToken(userId: Int): Future[Boolean] = {
    val userHasSessionAction = UserSessions.filter(_.userId === userId).result.headOption

    database.run(userHasSessionAction).map(notEmpty)
  }

  def getUserIdBySessionToken(userSessionToken: String): Future[Option[Int]] = {
    val getUserIdBySessionTokenAction = for {
      userSession <- UserSessions if userSession.userSessionToken === userSessionToken
    } yield userSession.userId

    database.run(getUserIdBySessionTokenAction.result.headOption)
  }

  def getSessionTokenByUserId(userId: Int): Future[Option[String]] = {
    val getSessionTokenByUserIdAction = for {
      userSession <- UserSessions if userSession.userId === userId
    } yield userSession.userSessionToken

    database.run(getSessionTokenByUserIdAction.result.headOption)
  }

  def deleteUserSession(userId: Int): Future[Boolean] = {
    val deleteUserSessionAction = UserSessions.filter(_.userId===userId).delete

    database.run(deleteUserSessionAction).map(isBiggerThanZero)
  }

  private def generateRandomToken(bitSize: Int): String = {
    val byteSize: Int = bitSize / 8
    val randomBytes: Array[Byte] = new Array[Byte](byteSize)

    secureRandom.nextBytes(randomBytes)
    base64Encoder.encodeToString(randomBytes)
  }

  def createPasswordResetToken(email: String): Future[String] = async {
    val userOption = await(getUserByEmail(email))

    userOption match {
      case Some(user) =>
        val passwordResetToken = generateRandomToken(passwordResetTokenBitSize)
        val passwordTokenCreated = await(insertPasswordTokenForUser(user.id, passwordResetToken))

        if (!passwordTokenCreated) {
          throw new SQLException(s"[createPasswordResetToken]: Failed to insert password reset token for email $email")
        }

        passwordResetToken
      case None => throw new SQLException(s"[createPasswordResetToken]: Could not find user with email $email")
    }
  }

  private def insertPasswordTokenForUser(userId: Int, passwordResetToken: String): Future[Boolean] = {
    val passwordTokenExpiration = new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(passwordRequestExpirationDurationMinutes))

    val newPasswordRequest = PasswordRequestsRow(-1, userId, passwordResetToken, passwordTokenExpiration)
    val insertPasswordRequestAction = PasswordRequests += newPasswordRequest
    val insertCountFuture = database.run(insertPasswordRequestAction)

    insertCountFuture.map(isBiggerThanZero)
  }

  def isPasswordResetTokenValid(passwordResetToken: String): Future[Boolean] = async {
    val getPasswordRequestAction = PasswordRequests.filter(_.passwordResetToken === passwordResetToken).result.headOption
    val passwordRequestOption = await(database.run(getPasswordRequestAction))

    passwordRequestOption.exists { passwordRequest =>
      val isTokenExpired = passwordRequest.passwordResetExpiration.getTime < System.currentTimeMillis()

      // if passwordRequest is expired delete without waiting
      if (isTokenExpired) {
        deletePasswordResetToken(passwordResetToken)
      }

      !isTokenExpired
    }
  }

  def updatePasswordWithPasswordResetToken(passwordResetToken: String, newPassword: String): Future[Boolean] = async {
    if (newPassword.isEmpty) {
      throw new SQLException(s"[updatePasswordWithPasswordResetToken]: empty password field")
    }

    val isTokenValid = await(isPasswordResetTokenValid(passwordResetToken))
    var passwordUpdated = false

    if (isTokenValid) {
      val userLinkedToTokenOption = await(getUserByPasswordToken(passwordResetToken))

      // user must exist at this point since token is valid and user_id field is not null
      if (userLinkedToTokenOption.nonEmpty) {
        passwordUpdated = await(updatePasswordForUser(userLinkedToTokenOption.get.id, newPassword))

        if (passwordUpdated) {
          deletePasswordResetToken(passwordResetToken)
        } else {
          throw new SQLException(s"[updatePasswordWithPasswordResetToken]: Failed to update password for reset token $passwordResetToken")
        }
      }
    }
    else {
      throw new SQLException(s"[updatePasswordWithPasswordResetToken]: passwordResetToken $passwordResetToken is invalid")
    }

    passwordUpdated
  }

  private def updatePasswordForUser(userId: Int, newPassword: String): Future[Boolean] = {
    val changePasswordForUserIdQuery = for {user <- Users if user.id === userId} yield user.password
    val encryptedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

    val changePasswordForUserIdAction = changePasswordForUserIdQuery.update(encryptedPassword)

    database.run(changePasswordForUserIdAction).map(isBiggerThanZero)
  }

  private def getUserByPasswordToken(passwordResetToken: String): Future[Option[UsersRow]] = {
    val getUserByTokenQuery = for {
      (user, passwordRequest) <- Users join PasswordRequests on (_.id === _.userId)
      if passwordRequest.passwordResetToken === passwordResetToken
    }
    yield user

    database.run(getUserByTokenQuery.result.headOption)
  }

  def userExists(username: String): Future[Boolean] = {
    getUserByUsername(username).map(notEmpty)
  }

  def getUserByUsername(username: String): Future[Option[UsersRow]] = {
    val getUserByUsernameAction = Users.filter(userRow => userRow.username === username).result.headOption

    database.run(getUserByUsernameAction)
  }

  def getUserByEmail(email: String): Future[Option[UsersRow]] = {
    val getUserByEmailQuery = for {user <- Users if user.email === email} yield user

    database.run(getUserByEmailQuery.result.headOption)
  }

  def getUserById(userId: Int): Future[Option[UsersRow]] = {
    val getUserByIdAction = Users.filter(_.id === userId).result.headOption

    database.run(getUserByIdAction)
  }

  def emailExists(email: String): Future[Boolean] = {
    getUserByEmail(email).map(notEmpty)
  }

  def deleteUserByUsername(username: String): Future[Boolean] = {
    val deleteUserByUsernameAction = Users.filter(userRow => userRow.username === username).delete

    database.run(deleteUserByUsernameAction).map(isBiggerThanZero)
  }

  def deleteUserByEmail(email: String): Future[Boolean] = {
    val deleteUserByEmailQuery = for {user <- Users if user.email === email} yield user

    database.run(deleteUserByEmailQuery.delete).map(isBiggerThanZero)
  }

  def deleteUserById(userId: Int): Future[Boolean] = {
    val deleteUserByIdAction = Users.filter(_.id === userId).delete

    database.run(deleteUserByIdAction).map(isBiggerThanZero)
  }

  private def deletePasswordResetToken(passwordResetToken: String): Future[Boolean] = {
    val deletePasswordResetTokenAction = PasswordRequests.filter(_.passwordResetToken === passwordResetToken).delete
    database.run(deletePasswordResetTokenAction).map(isBiggerThanZero)
  }
}