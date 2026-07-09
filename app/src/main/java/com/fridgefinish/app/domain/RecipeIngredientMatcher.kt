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
    return foodTermVariants(term).any { variant ->
        val pattern = Regex("(^|\\s)${Regex.escape(variant)}(\\s|$)")
        pattern.containsMatchIn(this)
    }
}

private fun foodTermVariants(term: String): Set<String> {
    val clean = term.normalizedFoodText()
    if (clean.isBlank()) return emptySet()

    val variants = mutableSetOf(clean)
    variants += when {
        clean.endsWith("y") -> clean.dropLast(1) + "ies"
        clean.endsWith("s") -> clean.dropLast(1)
        else -> clean + "s"
    }
    if (clean.endsWith("es")) variants += clean.dropLast(2)
    if (clean.endsWith("ies")) variants += clean.dropLast(3) + "y"
    return variants
}
