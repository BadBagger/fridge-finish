package com.fridgefinish.app.domain

fun recipeIngredientMatchesFood(
    ingredientLabel: String,
    ingredientKeywords: String,
    ingredientCategory: String?,
    foodName: String,
    foodCategory: FoodCategory
): Boolean {
    val classified = IngredientClassifier.classifyName(foodName, foodCategory)
    val name = classified.normalizedName
    val keywords = ingredientKeywords
        .split(",")
        .map { normalizeIngredientName(it) }
        .filter { it.length >= 2 }

    if (keywords.isEmpty()) return false

    val expectedCategories = IngredientClassifier.categoryFromRecipeCategory(ingredientCategory)
    if (IngredientCategory.LEFTOVER in expectedCategories && classified.isLeftover) {
        return true
    }

    if (name.looksLikePreparedFood()) {
        return keywords.any { keyword ->
            keyword in preparedFoodKeywords && name.containsWholeFoodTerm(keyword)
        }
    }

    if (keywords.any { keyword -> name.containsWholeFoodTerm(keyword) }) return true
    if (keywords.any { it in genericCategoryKeywords } && expectedCategories.any { it.compatibleWith(classified.category) }) return true
    if (expectedCategories.isNotEmpty() && !expectedCategories.any { it.compatibleWith(classified.category) }) return false

    return false
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

private fun IngredientCategory.compatibleWith(actual: IngredientCategory): Boolean =
    this == actual ||
        this == IngredientCategory.PROTEIN && actual == IngredientCategory.LEFTOVER ||
        this == IngredientCategory.LEFTOVER && actual == IngredientCategory.PROTEIN ||
        this == IngredientCategory.SAUCE && actual == IngredientCategory.CONDIMENT ||
        this == IngredientCategory.CONDIMENT && actual == IngredientCategory.SAUCE

private val genericCategoryKeywords = setOf(
    "protein",
    "meat",
    "vegetable",
    "fruit",
    "grain",
    "dairy",
    "sauce",
    "condiment",
    "spice",
    "snack",
    "leftover",
    "frozen",
    "canned",
    "baking",
    "drink"
)
