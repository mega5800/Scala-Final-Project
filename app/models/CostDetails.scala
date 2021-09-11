package models
import org.joda.time.DateTime
import models.Categories.Categories

class CostDetails(val costName: String, val purchaseDate: DateTime, val category: Categories, val costPrice: Float)