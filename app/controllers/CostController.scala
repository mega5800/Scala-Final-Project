package controllers

import controllers.actions.AuthenticatedAction
import controllers.utilties.{Attributes, FutureFailure, FutureResultHandler, FutureSuccess}
import models.CostsManagerModel
import models.Tables.UserItemCostsRow
import play.api.libs.concurrent.Futures
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import java.sql.{SQLException, Timestamp}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CostController @Inject()(authenticatedAction: AuthenticatedAction, costsManagerModel: CostsManagerModel, cc: ControllerComponents)(implicit executionContext: ExecutionContext, futures: Futures) extends AbstractController(cc) {
  def addItemCostPage(): Action[AnyContent] = authenticatedAction { implicit request =>
    Ok(views.html.addCost())
  }

  def addItemCost(): Action[AnyContent] = authenticatedAction.async { implicit request =>
    val userId = request.attrs(Attributes.UserID)
    val itemCostValues = request.body.asFormUrlEncoded.get
    print(itemCostValues)
    val itemCostToAdd = processAndCreateItemCost(userId, itemCostValues)

    val handler = FutureResultHandler(costsManagerModel.addSingleCostForUser(itemCostToAdd))

    handler.handle {
      case FutureSuccess(_) => Future.successful(Redirect(routes.CostController.addItemCost()).flashing("message" -> "Cost added successfully!"))
      case FutureFailure(exception) => exception match {
        case _: SQLException => Future.successful(Redirect(routes.CostController.addItemCost()).flashing("message" -> "Failed to add cost"))
      }
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

  // TODO: validate the fields, check if they're not empty etc
  private def processAndCreateItemCost(userId: Int, itemCostValues: Map[String, Seq[String]]): UserItemCostsRow = {
    val itemName = itemCostValues("itemName").head

    val purchaseDateString = itemCostValues("purchaseDate").head
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'hh:mm")
    val timeInMilliseconds = format.parse(purchaseDateString).getTime
    val purchaseDateTimestamp = new Timestamp(timeInMilliseconds)

    val category = itemCostValues("category").head
    val itemPriceString = itemCostValues("itemPrice").head
    val itemPrice = BigDecimal(itemPriceString)

    UserItemCostsRow(-1, userId, itemName, purchaseDateTimestamp, category, itemPrice)
  }
}
