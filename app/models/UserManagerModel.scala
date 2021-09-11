package models
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import models.Tables._
import scala.concurrent.Future
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.Await
import scala.concurrent.duration._

class UserManagerModel(private val database: Database)(implicit executionContext: ExecutionContext) {
    def validateUser(username: String, password: String): Future[Boolean] = {
        val user = Await.result(getUser(username), 5.seconds)

        Future(user != null && BCrypt.checkpw(password, user.password))
    }

    def createUser(username: String, password: String): Future[Boolean] = {
        val userFound = Await.result(userExists(username), 5.seconds)

        if(!userFound){
            // TODO: try catch to check if it added successfully
            // encrypt the password with default salt given by BCrypt library
            val encrpyedPassword: String = BCrypt.hashpw(password, BCrypt.gensalt())
            
            // passing negative 1 to automatically generate an id on the database
            val userToAdd: UsersRow = UsersRow(-1 , username, encrpyedPassword)
            val addUserQuery = Users += userToAdd // or use Users.insertOrUpdate(userToAdd)

            // run the query by the database and define the resolve
            database.run(addUserQuery)
            .map(addCount => addCount > 0)
        }
        else
        {
            Future(userFound)
        }
    }

    private def userExists(username: String): Future[Boolean] = {
        val user = Await.result(getUser(username), 5.seconds)

        Future(user != null)
    }

    private def getUser(username: String): Future[UsersRow] = {
        val users = Await.result(database.run(Users.filter(userRow => userRow.username === username).result), 5.seconds)
        
        Future(users.headOption.orNull)
    } 
}