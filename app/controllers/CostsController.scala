package controllers

import controllers.actions.AuthenticatedAction
import controllers.utilties.{Attributes, FutureFailure, FutureResultHandler, FutureSuccess}
import models.{Categories, CostsManagerModel}
import models.Tables.UserItemCostsRow
import play.api.libs.concurrent.Futures
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Flash}

import java.sql.{SQLException, Timestamp}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CostsController @Inject()(authenticatedAction: AuthenticatedAction, costsManagerModel: CostsManagerModel, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures) extends AbstractController(cc) {
  def addItemCostPage(): Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.addCost())
  }

  def addItemCost(): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val userId = request.attrs(Attributes.UserID)
    val itemCostValues = request.body.asFormUrlEncoded.get
    print(itemCostValues)

    processAndCreateItemCost(userId, itemCostValues) match {
      case Left(itemCostToAdd) =>
        val handler = FutureResultHandler(costsManagerModel.addSingleCostForUser(itemCostToAdd))

        handler.handle {
          case FutureSuccess(_) => Future.successful(Redirect(routes.CostsController.addItemCost()).flashing("message" -> "Cost added successfully!"))
          case FutureFailure(exception) => exception match {
            case _: SQLException => Future.successful(Redirect(routes.CostsController.addItemCost()).flashing("message" -> "Failed to add cost"))
          }
        }
      case Right(errorMessages) =>
        Future.successful(Redirect(routes.CostsController.addItemCost()).flashing("message" -> errorMessages))
    }
  }

  def deleteItemCost(): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val itemId = request.body.asFormUrlEncoded.get("deleteItem").head
    val userId = request.attrs(Attributes.UserID)

    val handler = FutureResultHandler(costsManagerModel.deleteCostForUser(userId, itemId.toInt))

    handler.handle {
      case FutureSuccess(itemDeleted) =>
        if(itemDeleted)
          Future.successful(Redirect(routes.HomeController.index).flashing("message" -> "Item deleted successfully!"))
        else
          Future.successful(Redirect(routes.HomeController.index).flashing("message" -> "Could not delete item, please try again later."))
    }
  }

  private def processAndCreateItemCost(userId: Int, itemCostValues: Map[String, Seq[String]], itemId: Int = -1): Either[UserItemCostsRow, String] = {
    var errorMessages: String = ""

    val itemName = itemCostValues("itemName").head
    val category = itemCostValues("category").head
    val purchaseDateString = itemCostValues("purchaseDate").head
    val itemPriceString = itemCostValues("itemPrice").head
    var purchaseDateTimestamp = Option.empty[Timestamp]
    var itemPrice = Option.empty[BigDecimal]

    if(itemName.isEmpty) {
      errorMessages =  errorMessages + "Item name must not be empty,"
    }

    if(purchaseDateString.nonEmpty) {
      purchaseDateTimestamp = convertDateTimeStringToTimestamp(purchaseDateString)

      if (purchaseDateTimestamp.isEmpty) {
        errorMessages = errorMessages + "Purchase date must be in the following format: yyyy-MM-dd'T'hh:mm,"
      }
    }

    if(!Categories.isCategory(category)){
      errorMessages = errorMessages + s"The category '$category' does not exist,"
    }

    val isNumeric = itemPriceString.matches("""\d+((\.)\d+)?""")

    if(isNumeric){
      itemPrice = Some(BigDecimal(itemPriceString))
    }
    else{
      errorMessages = errorMessages + "Item price must be a numeric type,"
    }

    if(errorMessages.isEmpty){
      Left(UserItemCostsRow(itemId, userId, itemName, purchaseDateTimestamp, category, itemPrice.get))
    }
    else{
      Right(errorMessages)
    }
  }

  private def convertDateTimeStringToTimestamp(dateTime: String): Option[Timestamp] = {
    var timestampResult = Option.empty[Timestamp]
    try {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'hh:mm")
      val timeInMilliseconds = format.parse(dateTime).getTime
      timestampResult = Some(new Timestamp(timeInMilliseconds))
    }
    catch{
      case _: IllegalArgumentException => println("Failed to convert date due to illegal format of dateTime input")
    }

    timestampResult
  }

  def editItemCost(): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val userId = request.attrs(Attributes.UserID)
    val itemCostValues = request.body.asFormUrlEncoded.get
    val itemId = itemCostValues("itemId").head

    processAndCreateItemCost(userId, itemCostValues, itemId.toInt) match {
      case Left(updatedItemCost) =>
        FutureResultHandler(costsManagerModel.updateCostForUser(updatedItemCost)).handle {
          case FutureSuccess(isUpdated) =>
            if(isUpdated){
              Future.successful(Redirect(routes.HomeController.index).flashing("message" -> "Item edited successfully!"))
            }
            else{
              Future.successful(Redirect(routes.HomeController.index).flashing("message" -> "Failed to edit item..."))
            }
        }
      case Right(errorMessages) => Future.successful(Redirect(routes.HomeController.index).flashing("message" -> errorMessages))
    }
  }
}
