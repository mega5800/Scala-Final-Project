import org.scalatest.time.Span
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.{Await, Awaitable, ExecutionContext}
import scala.concurrent.duration.DurationInt

abstract class DatabaseModelSpec extends PlaySpec{
  private val queryTime: Span = 2.seconds
  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  lazy val dbConfProvider: DatabaseConfigProvider = injector.instanceOf[DatabaseConfigProvider]
  implicit lazy val executionContext: ExecutionContext = injector.instanceOf[ExecutionContext]

  protected def await[DataType](awaitable: Awaitable[DataType]): DataType = {
    Await.result(awaitable, queryTime)
  }

  def cleanUp(): Unit
}
