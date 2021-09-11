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
  lazy val schema: profile.SchemaDescription = Costs.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Costs
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param userId Database column user_id SqlType(int4), Default(None)
   *  @param name Database column name SqlType(text)
   *  @param purchaseDate Database column purchase_date SqlType(date)
   *  @param category Database column category SqlType(text)
   *  @param costPrice Database column cost_price SqlType(numeric) */
  case class CostsRow(id: Int, userId: Option[Int] = None, name: String, purchaseDate: java.sql.Date, category: String, costPrice: scala.math.BigDecimal)
  /** GetResult implicit for fetching CostsRow objects using plain SQL queries */
  implicit def GetResultCostsRow(implicit e0: GR[Int], e1: GR[Option[Int]], e2: GR[String], e3: GR[java.sql.Date], e4: GR[scala.math.BigDecimal]): GR[CostsRow] = GR{
    prs => import prs._
    CostsRow.tupled((<<[Int], <<?[Int], <<[String], <<[java.sql.Date], <<[String], <<[scala.math.BigDecimal]))
  }
  /** Table description of table costs. Objects of this class serve as prototypes for rows in queries. */
  class Costs(_tableTag: Tag) extends profile.api.Table[CostsRow](_tableTag, "costs") {
    def * = (id, userId, name, purchaseDate, category, costPrice) <> (CostsRow.tupled, CostsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), userId, Rep.Some(name), Rep.Some(purchaseDate), Rep.Some(category), Rep.Some(costPrice))).shaped.<>({r=>import r._; _1.map(_=> CostsRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4), Default(None) */
    val userId: Rep[Option[Int]] = column[Option[Int]]("user_id", O.Default(None))
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column purchase_date SqlType(date) */
    val purchaseDate: Rep[java.sql.Date] = column[java.sql.Date]("purchase_date")
    /** Database column category SqlType(text) */
    val category: Rep[String] = column[String]("category")
    /** Database column cost_price SqlType(numeric) */
    val costPrice: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("cost_price")

    /** Foreign key referencing Users (database name costs_user_id_fkey) */
    lazy val usersFk = foreignKey("costs_user_id_fkey", userId, Users)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Costs */
  lazy val Costs = new TableQuery(tag => new Costs(tag))

  /** Entity class storing rows of table Users
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param username Database column username SqlType(varchar), Length(20,true)
   *  @param password Database column password SqlType(varchar), Length(200,true) */
  case class UsersRow(id: Int, username: String, password: String)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, "users") {
    def * = (id, username, password) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(username), Rep.Some(password))).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column username SqlType(varchar), Length(20,true) */
    val username: Rep[String] = column[String]("username", O.Length(20,varying=true))
    /** Database column password SqlType(varchar), Length(200,true) */
    val password: Rep[String] = column[String]("password", O.Length(200,varying=true))
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
