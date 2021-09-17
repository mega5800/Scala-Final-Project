package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = PasswordRequests.schema ++ UserItemCosts.schema ++ Users.schema ++ UserSessions.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table PasswordRequests
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param userId Database column user_id SqlType(int4)
   *  @param passwordResetToken Database column password_reset_token SqlType(text)
   *  @param passwordResetExpiration Database column password_reset_expiration SqlType(timestamp) */
  case class PasswordRequestsRow(id: Int, userId: Int, passwordResetToken: String, passwordResetExpiration: java.sql.Timestamp)
  /** GetResult implicit for fetching PasswordRequestsRow objects using plain SQL queries */
  implicit def GetResultPasswordRequestsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[PasswordRequestsRow] = GR{
    prs => import prs._
    PasswordRequestsRow.tupled((<<[Int], <<[Int], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table password_requests. Objects of this class serve as prototypes for rows in queries. */
  class PasswordRequests(_tableTag: Tag) extends profile.api.Table[PasswordRequestsRow](_tableTag, "password_requests") {
    def * = (id, userId, passwordResetToken, passwordResetExpiration) <> (PasswordRequestsRow.tupled, PasswordRequestsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userId), Rep.Some(passwordResetToken), Rep.Some(passwordResetExpiration))).shaped.<>({r=>import r._; _1.map(_=> PasswordRequestsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column password_reset_token SqlType(text) */
    val passwordResetToken: Rep[String] = column[String]("password_reset_token")
    /** Database column password_reset_expiration SqlType(timestamp) */
    val passwordResetExpiration: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("password_reset_expiration")

    /** Foreign key referencing Users (database name password_requests_user_id_fkey) */
    lazy val usersFk = foreignKey("password_requests_user_id_fkey", userId, Users)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (passwordResetToken) (database name password_requests_password_reset_token_key) */
    val index1 = index("password_requests_password_reset_token_key", passwordResetToken, unique=true)
  }
  /** Collection-like TableQuery object for table PasswordRequests */
  lazy val PasswordRequests = new TableQuery(tag => new PasswordRequests(tag))

  /** Entity class storing rows of table UserItemCosts
   *  @param itemId Database column item_id SqlType(int4)
   *  @param userId Database column user_id SqlType(int4)
   *  @param itemName Database column item_name SqlType(text)
   *  @param purchaseDate Database column purchase_date SqlType(timestamp)
   *  @param category Database column category SqlType(text)
   *  @param itemPrice Database column item_price SqlType(numeric) */
  case class UserItemCostsRow(itemId: Int, userId: Int, itemName: String, purchaseDate: java.sql.Timestamp, category: String, itemPrice: scala.math.BigDecimal)
  /** GetResult implicit for fetching UserItemCostsRow objects using plain SQL queries */
  implicit def GetResultUserItemCostsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Timestamp], e3: GR[scala.math.BigDecimal]): GR[UserItemCostsRow] = GR{
    prs => import prs._
    UserItemCostsRow.tupled((<<[Int], <<[Int], <<[String], <<[java.sql.Timestamp], <<[String], <<[scala.math.BigDecimal]))
  }
  /** Table description of table user_item_costs. Objects of this class serve as prototypes for rows in queries. */
  class UserItemCosts(_tableTag: Tag) extends profile.api.Table[UserItemCostsRow](_tableTag, "user_item_costs") {
    def * = (itemId, userId, itemName, purchaseDate, category, itemPrice) <> (UserItemCostsRow.tupled, UserItemCostsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(itemId), Rep.Some(userId), Rep.Some(itemName), Rep.Some(purchaseDate), Rep.Some(category), Rep.Some(itemPrice))).shaped.<>({r=>import r._; _1.map(_=> UserItemCostsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column item_id SqlType(int4) */
    val itemId: Rep[Int] = column[Int]("item_id")
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column item_name SqlType(text) */
    val itemName: Rep[String] = column[String]("item_name")
    /** Database column purchase_date SqlType(timestamp) */
    val purchaseDate: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("purchase_date")
    /** Database column category SqlType(text) */
    val category: Rep[String] = column[String]("category")
    /** Database column item_price SqlType(numeric) */
    val itemPrice: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("item_price")

    /** Primary key of UserItemCosts (database name user_item_costs_pkey) */
    val pk = primaryKey("user_item_costs_pkey", (userId, itemId))

    /** Foreign key referencing Users (database name user_item_costs_user_id_fkey) */
    lazy val usersFk = foreignKey("user_item_costs_user_id_fkey", userId, Users)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table UserItemCosts */
  lazy val UserItemCosts = new TableQuery(tag => new UserItemCosts(tag))

  /** Entity class storing rows of table Users
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param username Database column username SqlType(varchar), Length(20,true)
   *  @param password Database column password SqlType(varchar), Length(200,true)
   *  @param email Database column email SqlType(text)
   *  @param createdAt Database column created_at SqlType(timestamp) */
  case class UsersRow(id: Int, username: String, password: String, email: String, createdAt: Option[java.sql.Timestamp])
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[java.sql.Timestamp]]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[java.sql.Timestamp]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, "users") {
    def * = (id, username, password, email, createdAt) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(username), Rep.Some(password), Rep.Some(email), createdAt)).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column username SqlType(varchar), Length(20,true) */
    val username: Rep[String] = column[String]("username", O.Length(20,varying=true))
    /** Database column password SqlType(varchar), Length(200,true) */
    val password: Rep[String] = column[String]("password", O.Length(200,varying=true))
    /** Database column email SqlType(text) */
    val email: Rep[String] = column[String]("email")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("created_at")

    /** Uniqueness Index over (email) (database name users_email_key) */
    val index1 = index("users_email_key", email, unique=true)
    /** Uniqueness Index over (username) (database name users_username_key) */
    val index2 = index("users_username_key", username, unique=true)
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))

  /** Entity class storing rows of table UserSessions
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param userId Database column user_id SqlType(int4)
   *  @param userSessionToken Database column user_session_token SqlType(text) */
  case class UserSessionsRow(id: Int, userId: Int, userSessionToken: String)
  /** GetResult implicit for fetching UserSessionsRow objects using plain SQL queries */
  implicit def GetResultUserSessionsRow(implicit e0: GR[Int], e1: GR[String]): GR[UserSessionsRow] = GR{
    prs => import prs._
    UserSessionsRow.tupled((<<[Int], <<[Int], <<[String]))
  }
  /** Table description of table user_sessions. Objects of this class serve as prototypes for rows in queries. */
  class UserSessions(_tableTag: Tag) extends profile.api.Table[UserSessionsRow](_tableTag, "user_sessions") {
    def * = (id, userId, userSessionToken) <> (UserSessionsRow.tupled, UserSessionsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userId), Rep.Some(userSessionToken))).shaped.<>({r=>import r._; _1.map(_=> UserSessionsRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column user_session_token SqlType(text) */
    val userSessionToken: Rep[String] = column[String]("user_session_token")

    /** Foreign key referencing Users (database name user_sessions_user_id_fkey) */
    lazy val usersFk = foreignKey("user_sessions_user_id_fkey", userId, Users)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (userId) (database name user_sessions_user_id_key) */
    val index1 = index("user_sessions_user_id_key", userId, unique=true)
    /** Uniqueness Index over (userSessionToken) (database name user_sessions_user_session_token_key) */
    val index2 = index("user_sessions_user_session_token_key", userSessionToken, unique=true)
  }
  /** Collection-like TableQuery object for table UserSessions */
  lazy val UserSessions = new TableQuery(tag => new UserSessions(tag))
}
