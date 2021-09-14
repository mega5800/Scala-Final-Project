package controllers

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.libs.concurrent.Futures
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import play.api.libs.concurrent.Futures._
import play.api.mvc.Results.InternalServerError

import java.sql.SQLException
import scala.concurrent.duration.{DurationInt, FiniteDuration}


object FutureResultHandler {
  def apply[T](future: Future[T]): FutureResultHandler[T] = {
    new FutureResultHandler(future)
  }
}

class FutureResultHandler[T](private val future: Future[T]) {
  private val oopsMessage: String = "Oops, something went wrong!"
  private val maximumTimeout: FiniteDuration = 3.seconds

  def handle(onSuccess: T => Future[Result], onFailure: PartialFunction[Throwable, Future[Result]] = null)(implicit executionContext: ExecutionContext, futures: Futures): Future[Result] = {
    future.withTimeout(maximumTimeout)
      .transformWith {
        case Success(value) => onSuccess(value)
        case Failure(exception) =>
          println(exception.getMessage)

          if (onFailure != null && onFailure.isDefinedAt(exception)) {
            onFailure(exception)
          }
          else {
            Future.successful(InternalServerError(oopsMessage))
          }
      }
  }
}
