package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LocalTemplateRecipeGeneratorTest {
    private val today = LocalDate.of(2026, 7, 9)
    private val generator = LocalTemplateRecipeGenerator()

    @Test
    fun localGeneratorReturnsRichLocalTemplateSuggestions() {
        val suggestions = generator.generate(
            RecipeGeneratorInput(
                inventoryItems = listOf(
                    food("Chicken breast", FoodCategory.MEAT, today.plusDays(1)),
                    food("Rice", FoodCategory.PANTRY, today.plusMonths(2)),
                    food("Broccoli", FoodCategory.PRODUCE, today.plusDays(2)),
                    food("Soy sauce", FoodCategory.CONDIMENTS, today.plusMonths(6))
                ),
                today = today
            )
        )

        val bowl = suggestions.first { it.title.contains("Rice Bowl") }
        assertTrue(bowl.id.startsWith("local-template:"))
        assertEquals(RecipeGenerationSource.LOCAL_TEMPLATE, bowl.generatedBy)
        assertTrue(bowl.ingredientsOwned.any { it.contains("Chicken", ignoreCase = true) })
        assertTrue(bowl.ingredientsExpiringSoon.any { it.contains("Chicken", ignoreCase = true) })
        assertTrue(bowl.ingredientsMissing.isEmpty())
        assertTrue(bowl.steps.isNotEmpty())
        assertTrue(bowl.tags.contains("Quick meal") || bowl.tags.contains("Dinner"))
        assertTrue(bowl.explanation.isNotBlank())
    }

    @Test
    fun localGeneratorHonorsTimeAndMealTypeInput() {
        val suggestions = generator.generate(
            RecipeGeneratorInput(
                inventoryItems = listOf(
                    food("Eggs", FoodCategory.DAIRY, today.plusDays(2)),
                    food("Spinach", FoodCategory.PRODUCE, today.plusDays(1)),
                    food("Cheddar", FoodCategory.DAIRY, today.plusWeeks(2))
                ),
                availableTimeMinutes = 10,
                mealTypes = setOf(RecipeFilter.BREAKFAST),
                today = today
            )
        )

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all { it.estimatedTimeMinutes <= 10 })
        assertTrue(suggestions.all { RecipeFilter.BREAKFAST in it.filters })
    }

    @Test
    fun futureAiGeneratorIsPlaceholderOnly() {
        val suggestions = FutureAiRecipeGenerator().generate(
            RecipeGeneratorInput(inventoryItems = listOf(food("Rice", FoodCategory.PANTRY, today.plusMonths(1))), today = today)
        )

        assertTrue(suggestions.isEmpty())
    }

    private fun food(name: String, category: FoodCategory, expires: LocalDate): FoodItemEntity =
        FoodItemEntity(
            id = name.hashCode().toLong(),
            name = name,
            category = category,
            location = if (category == FoodCategory.PANTRY) FoodLocation.PANTRY else FoodLocation.FRIDGE,
            expirationDate = expires,
            reminderDaysBefore = FreshnessCalculator.defaultReminderDays(category)
        )
}
