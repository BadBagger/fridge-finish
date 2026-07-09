package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecipeSuggestionEngineTest {
    private val today = LocalDate.of(2026, 7, 8)

    @Test
    fun prioritizesExpiringFoodAndOwnedIngredients() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("Lettuce", FoodCategory.PRODUCE, today),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(3)),
                food("Milk", FoodCategory.DAIRY, today.plusDays(10))
            ),
            recipes = listOf(
                recipe(1, "Simple salad", 10, "Use greens", "Combine greens with dressing."),
                recipe(2, "Rice bowl", 20, "Use pantry base", "Warm rice with toppings.")
            ),
            ingredients = listOf(
                ingredient(1, "lettuce or spinach", "lettuce,spinach", "PRODUCE"),
                ingredient(1, "dressing", "dressing,sauce", "CONDIMENTS", required = false),
                ingredient(2, "rice", "rice", "PANTRY")
            ),
            today = today
        )

        assertEquals("Simple salad", suggestions.first().recipe.title)
        assertTrue(suggestions.first().expiringSoonFoods.any { it.name == "Lettuce" })
        assertTrue(suggestions.first().matchScore > suggestions.last().matchScore)
    }

    @Test
    fun prefersFewerMissingIngredients() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Eggs", FoodCategory.DAIRY, today.plusDays(2))),
            recipes = listOf(
                recipe(1, "Quick omelet", 15, "Use eggs", "Whisk eggs and cook."),
                recipe(2, "Full breakfast", 20, "Use eggs with extras", "Cook eggs with sides.")
            ),
            ingredients = listOf(
                ingredient(1, "eggs", "egg", "DAIRY"),
                ingredient(2, "eggs", "egg", "DAIRY"),
                ingredient(2, "toast", "bread,toast", "PANTRY"),
                ingredient(2, "fruit", "fruit,berry,banana", "PRODUCE")
            ),
            today = today
        )

        assertEquals("Quick omelet", suggestions.first().recipe.title)
        assertEquals(0, suggestions.first().missingRequired.size)
    }

    @Test
    fun appliesFilterTimeAndToolPreferences() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Yogurt", FoodCategory.DAIRY, today.plusDays(2))),
            recipes = listOf(
                recipe(1, "Yogurt fruit bowl", 5, "A fast way to use fruit and yogurt.", "Spoon yogurt into a bowl."),
                recipe(2, "Soup starter", 30, "Simmer vegetables.", "Simmer soup on the stove.")
            ),
            ingredients = listOf(
                ingredient(1, "yogurt", "yogurt", "DAIRY"),
                ingredient(2, "broth", "broth", "PANTRY")
            ),
            preferences = RecipeSuggestionPreferences(
                filters = setOf(RecipeFilter.BREAKFAST, RecipeFilter.NO_COOK),
                maxMinutes = 5,
                tools = setOf(CookingTool.NO_TOOLS)
            ),
            today = today
        )

        assertEquals(listOf("Yogurt fruit bowl"), suggestions.map { it.recipe.title })
    }

    @Test
    fun warnsWhenPastDateFoodIsUsed() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken leftovers", FoodCategory.LEFTOVERS, today.minusDays(1))),
            recipes = listOf(recipe(1, "Leftover grain bowl", 12, "Use leftovers first.", "Warm leftovers with rice.")),
            ingredients = listOf(ingredient(1, "leftovers", "leftover,chicken", "LEFTOVERS")),
            today = today
        )

        assertTrue(suggestions.single().safetyNote.contains("past-date", ignoreCase = true))
    }

    private fun food(name: String, category: FoodCategory, expires: LocalDate): FoodItemEntity =
        FoodItemEntity(
            id = name.hashCode().toLong(),
            name = name,
            category = category,
            location = FoodLocation.FRIDGE,
            expirationDate = expires,
            reminderDaysBefore = FreshnessCalculator.defaultReminderDays(category)
        )

    private fun recipe(id: Long, title: String, minutes: Int, description: String, steps: String): RecipeEntity =
        RecipeEntity(id = id, title = title, minutes = minutes, description = description, steps = steps, sourceName = "Test")

    private fun ingredient(
        recipeId: Long,
        label: String,
        keywords: String,
        category: String,
        required: Boolean = true
    ): RecipeIngredientEntity =
        RecipeIngredientEntity(recipeId = recipeId, label = label, keywords = keywords, category = category, required = required)
}
