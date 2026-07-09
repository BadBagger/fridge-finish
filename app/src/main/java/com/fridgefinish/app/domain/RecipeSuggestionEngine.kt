package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RecipeFilter(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    QUICK("Quick meal"),
    LEFTOVER_RESCUE("Leftover rescue"),
    NO_COOK("No-cook"),
    ONE_PAN("One-pan"),
    HEALTHY("Healthy"),
    COMFORT("Comfort food")
}

enum class CookingTool(val label: String) {
    MICROWAVE("Microwave"),
    OVEN("Oven"),
    STOVE("Stove"),
    AIR_FRYER("Air fryer"),
    SLOW_COOKER("Slow cooker"),
    NO_TOOLS("No tools")
}

data class RecipeSuggestionPreferences(
    val filters: Set<RecipeFilter> = emptySet(),
    val maxMinutes: Int? = null,
    val tools: Set<CookingTool> = emptySet()
)

data class RecipeSuggestion(
    val recipe: RecipeEntity,
    val usedFoods: List<FoodItemEntity>,
    val requiredMatchedFoods: List<FoodItemEntity>,
    val optionalMatchedFoods: List<FoodItemEntity>,
    val expiringSoonFoods: List<FoodItemEntity>,
    val missingRequired: List<RecipeIngredientEntity>,
    val matchScore: Int,
    val whySuggested: String,
    val difficulty: String,
    val filters: Set<RecipeFilter>,
    val tools: Set<CookingTool>,
    val safetyNote: String
)

object RecipeSuggestionEngine {
    fun buildSuggestions(
        foods: List<FoodItemEntity>,
        recipes: List<RecipeEntity>,
        ingredients: List<RecipeIngredientEntity>,
        preferences: RecipeSuggestionPreferences = RecipeSuggestionPreferences(),
        today: LocalDate = LocalDate.now()
    ): List<RecipeSuggestion> {
        val activeFoods = foods
            .filterNot { it.isFinished }
            .sortedBy { FreshnessCalculator.urgencyRank(it.expirationDate, it.reminderDaysBefore, it.isFinished, today) }
        val ingredientsByRecipe = ingredients.groupBy { it.recipeId }

        return recipes.mapNotNull { recipe ->
            val traits = recipe.inferFilters()
            val tools = recipe.inferTools()
            if (preferences.filters.isNotEmpty() && !traits.containsAll(preferences.filters)) return@mapNotNull null
            if (preferences.maxMinutes != null && recipe.minutes > preferences.maxMinutes) return@mapNotNull null
            if (preferences.tools.isNotEmpty() && preferences.tools.intersect(tools).isEmpty()) return@mapNotNull null

            val recipeIngredients = ingredientsByRecipe[recipe.id].orEmpty()
            if (recipeIngredients.isEmpty()) return@mapNotNull null

            val matched = recipeIngredients.mapNotNull { ingredient ->
                val food = activeFoods.firstOrNull { ingredient.matches(it) }
                if (food == null) null else ingredient to food
            }
            val usedFoods = matched.map { it.second }.distinctBy { it.id }
            if (usedFoods.isEmpty()) return@mapNotNull null

            val missingRequired = recipeIngredients
                .filter { ingredient -> ingredient.required && matched.none { match -> match.first == ingredient } }
                .distinctBy { it.shoppingKey() }
            if (missingRequired.size > 3) return@mapNotNull null

            val expiringSoon = usedFoods.filter { food ->
                FreshnessCalculator.status(food.expirationDate, food.reminderDaysBefore, food.isFinished, today) in urgentStatuses
            }
            val score = scoreRecipe(recipe, usedFoods, missingRequired, expiringSoon, today)
            RecipeSuggestion(
                recipe = recipe,
                usedFoods = usedFoods,
                requiredMatchedFoods = matched.filter { it.first.required }.map { it.second }.distinctBy { it.id },
                optionalMatchedFoods = matched.filterNot { it.first.required }.map { it.second }.distinctBy { it.id },
                expiringSoonFoods = expiringSoon,
                missingRequired = missingRequired,
                matchScore = score,
                whySuggested = why(recipe, usedFoods, missingRequired, expiringSoon, today),
                difficulty = recipe.difficulty(),
                filters = traits,
                tools = tools,
                safetyNote = safetyNote(usedFoods, today)
            )
        }.sortedWith(
            compareByDescending<RecipeSuggestion> { it.matchScore }
                .thenBy { it.missingRequired.size }
                .thenByDescending { it.expiringSoonFoods.size }
                .thenBy { it.recipe.minutes }
        )
    }

    private fun scoreRecipe(
        recipe: RecipeEntity,
        usedFoods: List<FoodItemEntity>,
        missingRequired: List<RecipeIngredientEntity>,
        expiringSoon: List<FoodItemEntity>,
        today: LocalDate
    ): Int {
        val soonScore = expiringSoon.fold(0) { total, food ->
            total + when (FreshnessCalculator.status(food.expirationDate, food.reminderDaysBefore, food.isFinished, today)) {
                FreshnessStatus.EXPIRED -> 16
                FreshnessStatus.EXPIRES_TODAY -> 22
                FreshnessStatus.EAT_SOON -> 14
                else -> 0
            }
        }
        val ownedScore = (usedFoods.size * 8).coerceAtMost(28)
        val missingPenalty = missingRequired.size * 14
        val timeBonus = when {
            recipe.minutes <= 5 -> 10
            recipe.minutes <= 15 -> 8
            recipe.minutes <= 30 -> 4
            else -> 0
        }
        val pantryBonus = if (usedFoods.any { it.category == FoodCategory.PANTRY }) 4 else 0
        return (42 + soonScore + ownedScore + timeBonus + pantryBonus - missingPenalty).coerceIn(0, 100)
    }

    private fun why(
        recipe: RecipeEntity,
        usedFoods: List<FoodItemEntity>,
        missingRequired: List<RecipeIngredientEntity>,
        expiringSoon: List<FoodItemEntity>,
        today: LocalDate
    ): String {
        val urgent = expiringSoon.minByOrNull { ChronoUnit.DAYS.between(today, it.expirationDate) }
        return when {
            urgent != null && missingRequired.isEmpty() -> "Uses ${urgent.name.cleanShoppingName()} before its date, and you already have everything needed."
            urgent != null -> "Uses ${urgent.name.cleanShoppingName()} soon and only needs ${missingRequired.size} missing item${if (missingRequired.size == 1) "" else "s"}."
            missingRequired.isEmpty() -> "You already have the main ingredients, and it takes ${recipe.minutes} minutes."
            else -> "Good match for what you have with ${missingRequired.size} item${if (missingRequired.size == 1) "" else "s"} to add."
        }
    }

    private fun safetyNote(foods: List<FoodItemEntity>, today: LocalDate): String {
        val hasExpired = foods.any { it.expirationDate.isBefore(today) }
        val hasLeftovers = foods.any { it.category == FoodCategory.LEFTOVERS || it.name.contains("leftover", ignoreCase = true) }
        return when {
            hasExpired -> "Includes a past-date item. Check before eating and use your judgment."
            hasLeftovers -> "For leftovers, reheat thoroughly and check smell, texture, and date before eating."
            else -> "Dates are reminders, not safety guarantees. Check food before eating."
        }
    }

    private fun RecipeEntity.inferFilters(): Set<RecipeFilter> {
        val text = "$title $description $steps".lowercase()
        return buildSet {
            if (minutes <= 15) add(RecipeFilter.QUICK)
            if (listOf("omelet", "yogurt", "smoothie").any { text.contains(it) }) add(RecipeFilter.BREAKFAST)
            if (listOf("salad", "grain bowl", "quesadilla", "soup").any { text.contains(it) }) add(RecipeFilter.LUNCH)
            if (listOf("pasta", "rice", "soup", "quesadilla", "omelet").any { text.contains(it) }) add(RecipeFilter.DINNER)
            if (listOf("snack", "plate", "yogurt", "smoothie").any { text.contains(it) }) add(RecipeFilter.SNACK)
            if (text.contains("leftover")) add(RecipeFilter.LEFTOVER_RESCUE)
            if (listOf("smoothie", "yogurt", "salad", "snack").any { text.contains(it) }) add(RecipeFilter.NO_COOK)
            if (listOf("omelet", "pasta", "rice", "soup", "quesadilla").any { text.contains(it) }) add(RecipeFilter.ONE_PAN)
            if (listOf("salad", "smoothie", "yogurt", "vegetable", "greens", "fruit").any { text.contains(it) }) add(RecipeFilter.HEALTHY)
            if (listOf("pasta", "quesadilla", "soup", "fried rice", "cheese").any { text.contains(it) }) add(RecipeFilter.COMFORT)
        }
    }

    private fun RecipeEntity.inferTools(): Set<CookingTool> {
        val text = "$title $description $steps".lowercase()
        return buildSet {
            if (listOf("smoothie", "yogurt", "salad", "snack plate").any { text.contains(it) }) add(CookingTool.NO_TOOLS)
            if (listOf("warm", "leftover").any { text.contains(it) }) add(CookingTool.MICROWAVE)
            if (listOf("omelet", "pasta", "soup", "fried rice", "quesadilla", "stir-fry", "cook", "simmer", "toast").any { text.contains(it) }) add(CookingTool.STOVE)
            if (listOf("pizza", "bake", "roast").any { text.contains(it) }) add(CookingTool.OVEN)
            if (listOf("air fryer", "crispy").any { text.contains(it) }) add(CookingTool.AIR_FRYER)
            if (listOf("slow cooker", "slow-cooker").any { text.contains(it) }) add(CookingTool.SLOW_COOKER)
        }.ifEmpty { setOf(CookingTool.STOVE) }
    }

    private fun RecipeEntity.difficulty(): String = when {
        minutes <= 10 -> "Easy"
        minutes <= 30 -> "Moderate"
        else -> "More involved"
    }

    private fun RecipeIngredientEntity.matches(food: FoodItemEntity): Boolean =
        recipeIngredientMatchesFood(label, keywords, category, food.name, food.category)

    private fun RecipeIngredientEntity.shoppingKey(): String = label.trim().lowercase()

    private val urgentStatuses = setOf(
        FreshnessStatus.EXPIRED,
        FreshnessStatus.EXPIRES_TODAY,
        FreshnessStatus.EAT_SOON
    )
}
