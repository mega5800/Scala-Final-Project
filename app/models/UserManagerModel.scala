package models

import models.Tables._
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import java.security.SecureRandom
import java.sql.{Blob, SQLException, Timestamp}
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.sql.rowset.serial.SerialBlob
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

// TODO: Ensure all database queries successfully complete (database could be down)
class UserManagerModel @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends DatabaseModel(dbConfigProvider) {
  private val secureRandom: SecureRandom = new SecureRandom()
  private val base64Encoder: Base64.Encoder = Base64.getUrlEncoder
  private val passwordRequestExpirationDurationMinutes = 30

  def validateUser(username: String, password: String): Future[Boolean] = {
    val userFuture = getUserByUsername(username)

    userFuture.map { user =>
      user.nonEmpty && BCrypt.checkpw(password, user.get.password)
    }
  }

  def createUser(username: String, password: String, email: String): Future[Int] = {
    // encrypt the password with default salt given by BCrypt library
    val encryptedPassword: String = BCrypt.hashpw(password, BCrypt.gensalt())

    // passing negative 1 to automatically generate an id on the database
    val userToAdd: UsersRow = UsersRow(-1, username, encryptedPassword, email)
    val addUserQuery = (Users returning Users.map(_.id)) += userToAdd // or use Users.insertOrUpdate(userToAdd)

    // run the query by the database
    database.run(addUserQuery)
  }

  private def generatePasswordResetToken(): String = {
    val tokenSize = 11
    val randomBytes: Array[Byte] = new Array[Byte](tokenSize)

    secureRandom.nextBytes(randomBytes)
    base64Encoder.encodeToString(randomBytes)
  }

  def createPasswordResetToken(email: String): Future[String] = async {
    val userOption = await(getUserByEmail(email))

    userOption match {
      case Some(user) =>
        val passwordResetToken = generatePasswordResetToken()
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
    if(newPassword.isEmpty){
      throw new SQLException(s"[updatePasswordWithPasswordResetToken]: empty password field")
    }

    val isTokenValid = await(isPasswordResetTokenValid(passwordResetToken))
    var passwordUpdated = false

    if (isTokenValid) {
      val userLinkedToTokenOption = await(getUserByPasswordToken(passwordResetToken))

      // user must exist at this point since token is valid and user_id field is not null
      if(userLinkedToTokenOption.nonEmpty){
        passwordUpdated = await(updatePasswordForUser(userLinkedToTokenOption.get.id, newPassword))

        if (passwordUpdated) {
          deletePasswordResetToken(passwordResetToken)
        } else {
          throw new SQLException(s"[updatePasswordWithPasswordResetToken]: Failed to update password for reset token $passwordResetToken")
        }
      }
    }
    else{
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