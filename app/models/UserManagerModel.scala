package models
import akka.http.scaladsl.model.DateTime
import ch.qos.logback.core.pattern.color.BoldCyanCompositeConverter

import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import models.Tables._
import org.mindrot.jbcrypt.BCrypt

import java.security.SecureRandom
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import java.util.concurrent.TimeUnit
import java.sql.Timestamp
import java.util.Base64

// TODO: Ensure all database queries successfully complete (database could be down)
class UserManagerModel(private val database: Database)(implicit executionContext: ExecutionContext) {
    private val queryTimeout = 3.seconds
    private val secureRandom: SecureRandom = new SecureRandom()
    private val base64Encoder :Base64.Encoder = Base64.getUrlEncoder

    def validateUser(username: String, password: String): Future[Boolean] = {
        val user = Await.result(getUser(username), queryTimeout)

        Future.successful(user.nonEmpty && BCrypt.checkpw(password, user.get.password))
    }

    def createUser(username: String, password: String, email: String): Future[Boolean] = {
        // encrypt the password with default salt given by BCrypt library
        val encryptedPassword: String = BCrypt.hashpw(password, BCrypt.gensalt())

        // passing negative 1 to automatically generate an id on the database
        val userToAdd: UsersRow = UsersRow(-1 , username, encryptedPassword, email)
        val addUserQuery = Users += userToAdd // or use Users.insertOrUpdate(userToAdd)

        // run the query by the database and define the resolve
        database.run(addUserQuery.asTry).map {
            case Success(addCount) => addCount > 0
            case Failure(exception) =>
                println(exception.getMessage)
                false
        }
    }

    private def generatePasswordResetToken(): String = {
        val tokenSize = 11;
        val randomBytes: Array[Byte] = new Array[Byte](tokenSize)

        secureRandom.nextBytes(randomBytes)
        Base64.getUrlEncoder.encodeToString(randomBytes)
    }

    def getUserByEmail(email: String): Future[Option[UsersRow]] = {
        val getUserByEmailQuery = for {user <- Users if user.email === email} yield user

        database.run(getUserByEmailQuery.result.headOption)
    }

    def createPasswordResetToken(email: String): Future[Option[String]] = {
        val user = Await.result(getUserByEmail(email), queryTimeout)
        var tokenToReturn = Option.empty[String]

        if(user.nonEmpty)
        {
            val passwordResetToken = generatePasswordResetToken()
            val passwordTokenExpiration = new Timestamp(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30))
            tokenToReturn = Some(passwordResetToken)

            database.run(PasswordRequests += PasswordRequestsRow(-1, user.get.id, passwordResetToken, passwordTokenExpiration))
        }

        Future.successful(tokenToReturn)
    }

    def isPasswordResetTokenValid(passwordResetToken: String): Future[Boolean] = {
        val getPasswordRequestAction = PasswordRequests.filter(_.passwordResetToken === passwordResetToken).result.headOption
        database.run(getPasswordRequestAction).map(passwordRequestOption => passwordRequestOption.exists {
            passwordRequest => {
                val resetTokenExpired = passwordRequest.passwordResetExpiration.getTime < System.currentTimeMillis()

                // if passwordRequest is expired delete it without blocking the thread
                if(resetTokenExpired) deletePasswordResetToken(passwordResetToken)

                !resetTokenExpired
            }
        })
    }

    def resetPassword(passwordResetToken: String, newPassword: String): Future[Boolean] = {
        var passwordChanged = false

        if(Await.result(isPasswordResetTokenValid(passwordResetToken), queryTimeout)){
            val getIdOfUser =  for {
                (user, passwordRequest) <- Users join PasswordRequests on (_.id === _.userId)
                if passwordRequest.passwordResetToken === passwordResetToken
            }
            yield user.id

            // Result is Option[Int], however there is no need to check because of isPasswordResetTokenValid checking the existence passwordResetToken
            val userId = await(database.run(getIdOfUser.result.headOption)).get
            val changePasswordForUserIdQuery = for { user <- Users if user.id === userId } yield user.password
            val encryptedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
            val changePasswordForUserIdAction = changePasswordForUserIdQuery.update(encryptedPassword)

            passwordChanged = await(database.run(changePasswordForUserIdAction).map(updateCount => updateCount > 0))

            if(passwordChanged) deletePasswordResetToken(passwordResetToken)
        }

        Future.successful(passwordChanged)
    }

    private def userExists(username: String): Future[Boolean] = {
        val user = Await.result(getUser(username), queryTimeout)

        Future.successful(user.nonEmpty)
    }

    private def getUser(username: String): Future[Option[UsersRow]] = {
        val getUserAction = Users.filter(userRow => userRow.username === username).result
        val users = Await.result(database.run(getUserAction), queryTimeout)
        
        Future.successful(users.headOption)
    }

    private def emailExists(email: String): Future[Boolean] = {
        val emailExistsQuery = Users.filter(userRow => userRow.email === email).result
        val userRows = Await.result(database.run(emailExistsQuery), queryTimeout)

        Future.successful(userRows.nonEmpty)
    }
    
    private def await[ResultType](awaitable: Awaitable[ResultType]): ResultType = {
        Await.result(awaitable, queryTimeout)
    }

    private def deletePasswordResetToken(passwordResetToken: String): Future[Boolean] ={
        database.run(PasswordRequests.filter(_.passwordResetToken === passwordResetToken).delete).map(deleteCount => deleteCount > 0)
    }
}