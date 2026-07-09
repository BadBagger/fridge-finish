package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeFeedbackEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import java.time.Instant
import java.time.LocalDate
import kotlin.math.absoluteValue

interface RecipeGenerator {
    fun generate(input: RecipeGeneratorInput): List<RecipeSuggestion>
}

data class RecipeGeneratorInput(
    val inventoryItems: List<FoodItemEntity>,
    val expiringItems: List<FoodItemEntity> = emptyList(),
    val userPreferences: RecipeSuggestionPreferences = RecipeSuggestionPreferences(),
    val availableTimeMinutes: Int? = userPreferences.maxMinutes,
    val mealTypes: Set<RecipeFilter> = userPreferences.filters,
    val cookingTools: Set<CookingTool> = userPreferences.tools,
    val assumePantryStaples: Boolean = userPreferences.assumePantryStaples,
    val dietaryRestrictions: Set<String> = userPreferences.dietaryRestrictions,
    val feedbackHistory: List<RecipeFeedbackEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeIngredients: List<RecipeIngredientEntity> = emptyList(),
    val today: LocalDate = LocalDate.now()
) {
    fun resolvedPreferences(): RecipeSuggestionPreferences =
        userPreferences.copy(
            filters = mealTypes,
            maxMinutes = availableTimeMinutes,
            tools = cookingTools,
            assumePantryStaples = assumePantryStaples,
            dietaryRestrictions = dietaryRestrictions,
            recentlyHiddenTitles = userPreferences.recentlyHiddenTitles +
                feedbackHistory.filter { it.action.name == "HIDDEN" }.map { it.recipeTitle }
        )
}

class LocalTemplateRecipeGenerator : RecipeGenerator {
    override fun generate(input: RecipeGeneratorInput): List<RecipeSuggestion> {
        val preferences = input.resolvedPreferences()
        val librarySuggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = input.inventoryItems,
            recipes = input.recipes,
            ingredients = input.recipeIngredients,
            preferences = preferences,
            today = input.today
        )
        val templateSuggestions = RecipeTemplateEngine.buildSuggestions(
            foods = input.inventoryItems,
            preferences = preferences,
            today = input.today
        ).map { it.toRecipeSuggestion(input.today) }

        return (templateSuggestions + librarySuggestions)
            .distinctBy { it.title.lowercase() }
            .sortedWith(
                compareByDescending<RecipeSuggestion> { it.matchScore }
                    .thenBy { it.ingredientsMissing.size }
                    .thenByDescending { it.ingredientsExpiringSoon.size }
                    .thenBy { it.estimatedTimeMinutes }
            )
    }

    private fun TemplateRecipeSuggestion.toRecipeSuggestion(today: LocalDate): RecipeSuggestion {
        val recipeId = -templateId.hashCode().absoluteValue.toLong().coerceAtLeast(1L)
        val syntheticRecipe = RecipeEntity(
            id = recipeId,
            title = title,
            minutes = minutes,
            description = reason,
            steps = steps.joinToString(". "),
            sourceName = RecipeGenerationSource.LOCAL_TEMPLATE.label,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val missingRequired = missingIngredients.mapIndexed { index, missing ->
            RecipeIngredientEntity(
                id = -(recipeId.absoluteValue + index + 1),
                recipeId = recipeId,
                label = missing,
                keywords = normalizeIngredientName(missing),
                category = null,
                required = true
            )
        }
        val requiredMatched = usedFoods.filterNot { used ->
            optionalMatchedFoods.any { optional -> optional.id == used.id }
        }
        val safetyNote = localSafetyNote(usedFoods, today)
        return RecipeSuggestion(
            recipe = syntheticRecipe,
            usedFoods = usedFoods,
            requiredMatchedFoods = requiredMatched,
            optionalMatchedFoods = optionalMatchedFoods,
            expiringSoonFoods = expiringSoonFoods,
            missingRequired = missingRequired,
            matchScore = matchScore,
            scoreReasons = scoreReasons,
            whySuggested = reason,
            difficulty = difficulty,
            filters = filters,
            tools = tools,
            safetyNote = safetyNote,
            id = "local-template:$templateId:${normalizeIngredientName(title)}",
            title = title,
            summary = reason,
            ingredientsOwned = usedFoods.map { it.name.cleanShoppingName() }.distinct(),
            ingredientsExpiringSoon = expiringSoonFoods.map { it.name.cleanShoppingName() }.distinct(),
            ingredientsMissing = missingIngredients.map { it.cleanShoppingName() }.distinct(),
            ingredientsOptional = optionalMatchedFoods.map { it.name.cleanShoppingName() }.distinct(),
            steps = steps,
            estimatedTimeMinutes = minutes,
            tags = filters.map { it.label }.toSet() + tools.map { it.label },
            safetyWarnings = listOf(safetyNote),
            generatedBy = RecipeGenerationSource.LOCAL_TEMPLATE,
            explanation = reason
        )
    }

    private fun localSafetyNote(foods: List<FoodItemEntity>, today: LocalDate): String =
        when {
            foods.any { it.itemState == FoodItemState.SPOILED } -> "Spoiled items are not recipe ingredients. Dispose of them instead."
            foods.any { it.itemState == FoodItemState.QUESTIONABLE } -> "Use only if still safe. Check smell, texture, and package guidance before using."
            foods.any { it.itemState == FoodItemState.EXPIRED || it.expirationDate.isBefore(today) } -> "Includes a past-date item. Check smell, texture, and package guidance before using."
            foods.any { it.isAgedLeftover(today) } -> "Leftovers can become risky after several days. Check smell, texture, date, and reheat thoroughly."
            foods.any { it.isHighRiskFood() } -> "High-risk foods need extra caution. Check smell, texture, and package guidance before using."
            else -> "Dates are reminders, not safety guarantees. Check food before eating."
        }
}

class FutureAiRecipeGenerator : RecipeGenerator {
    override fun generate(input: RecipeGeneratorInput): List<RecipeSuggestion> = emptyList()
}
