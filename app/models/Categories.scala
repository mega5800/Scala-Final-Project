package models

object Categories extends Enumeration {
    type Categories = Value
    val Sports, Food, Lifestyle, Tech, VideoGames, Home = Value

    def isCategory(potentialCategory: String): Boolean = values.exists(category => category.toString.toLowerCase() == potentialCategory.toLowerCase())
}