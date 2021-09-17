package controllers.utilties

import play.api.libs.concurrent.Futures
import play.api.libs.concurrent.Futures._
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success}

object FutureResultHandler {
  def apply[T](future: Future[T]): FutureResultHandler[T] = {
    new FutureResultHandler[T](future)
  }
}

class FutureResultHandler[T](private val future: Future[T]) {
  private val oopsMessage: String = "Oops, something went wrong!"
  private val maximumTimeout: FiniteDuration = 3.seconds

  def handle(futureStatusFunction: PartialFunction[FutureStatus[T], Future[Result]])(implicit futures: Futures): Future[Result] = {
    future.withTimeout(maximumTimeout)
      .transformWith {
        case Success(value) => futureStatusFunction(FutureSuccess(value))
        case Failure(exception) =>
          println(exception.getMessage)
          val futureFailure = FutureFailure[T](exception)

          if(futureStatusFunction.isDefinedAt(futureFailure)){
            try{
              futureStatusFunction(futureFailure)
            }
            catch {
              case exception: Throwable =>
                print("Unknown exception: " + exception.getMessage)
                Future.successful(InternalServerError(oopsMessage))
            }
          }
          else{
            Future.successful(InternalServerError(oopsMessage))
          }
      }
  }
}

sealed abstract class FutureStatus[T]
final case class FutureSuccess[T](value: T) extends FutureStatus[T]
final case class FutureFailure[T](exception: Throwable) extends FutureStatus[T]