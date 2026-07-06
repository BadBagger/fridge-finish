package com.fridgefinish.app.domain

fun recipeIngredientMatchesFood(
    ingredientLabel: String,
    ingredientKeywords: String,
    ingredientCategory: String?,
    foodName: String,
    foodCategory: FoodCategory
): Boolean {
    val name = foodName.normalizedFoodText()
    val keywords = ingredientKeywords
        .split(",")
        .map { it.normalizedFoodText() }
        .filter { it.length >= 2 }

    if (keywords.isEmpty()) return false

    val category = ingredientCategory?.trim()?.uppercase()
    if (category == FoodCategory.LEFTOVERS.name && foodCategory == FoodCategory.LEFTOVERS) {
        return true
    }

    if (name.looksLikePreparedFood()) {
        return keywords.any { keyword ->
            keyword in preparedFoodKeywords && name.containsWholeFoodTerm(keyword)
        }
    }

    return keywords.any { keyword -> name.containsWholeFoodTerm(keyword) }
}

private val preparedFoodNames = setOf(
    "burrito",
    "dumpling",
    "lasagna",
    "meal",
    "pierogi",
    "pizza",
    "pot pie",
    "quesadilla",
    "ravioli",
    "sandwich",
    "wrap"
)

private val preparedFoodKeywords = setOf(
    "burrito",
    "dumpling",
    "lasagna",
    "noodle",
    "pasta",
    "pierogi",
    "pizza",
    "pot pie",
    "quesadilla",
    "ravioli",
    "sandwich",
    "wrap"
)

private fun String.looksLikePreparedFood(): Boolean =
    preparedFoodNames.any { containsWholeFoodTerm(it) }

private fun String.normalizedFoodText(): String =
    lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun String.containsWholeFoodTerm(term: String): Boolean {
    val pattern = Regex("(^|\\s)${Regex.escape(term)}(\\s|$)")
    return pattern.containsMatchIn(this)
}
