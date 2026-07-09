package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecipeTemplateEngineTest {
    private val today = LocalDate.of(2026, 7, 8)

    @Test
    fun generatesChickenBroccoliRiceBowlFromTemplateSlots() {
        val suggestions = RecipeTemplateEngine.buildSuggestions(
            foods = listOf(
                food("Chicken breast", FoodCategory.MEAT, today.plusDays(1)),
                food("Broccoli", FoodCategory.PRODUCE, today.plusDays(2)),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(3)),
                food("Soy sauce", FoodCategory.CONDIMENTS, today.plusMonths(6))
            ),
            today = today
        )

        val bowl = suggestions.first { it.templateId == "rice_bowl" }
        assertEquals("Chicken Broccoli Rice Bowl", bowl.title)
        assertTrue(bowl.missingIngredients.isEmpty())
        assertTrue(bowl.steps.any { it.contains("Warm rice") })
        assertTrue(bowl.reason.contains("Chicken breast", ignoreCase = true))
    }

    @Test
    fun generatesSpinachCheeseEggScramble() {
        val scramble = RecipeTemplateEngine.buildSuggestions(
            foods = listOf(
                food("Eggs", FoodCategory.DAIRY, today.plusDays(2)),
                food("Spinach", FoodCategory.PRODUCE, today.plusDays(1)),
                food("Cheddar", FoodCategory.DAIRY, today.plusWeeks(2))
            ),
            today = today
        ).first { it.templateId == "scramble" }

        assertEquals("Spinach Cheddar Egg Scramble", scramble.title)
        assertTrue(scramble.missingIngredients.isEmpty())
        assertEquals(10, scramble.minutes)
        assertTrue(CookingTool.STOVE in scramble.tools)
    }

    @Test
    fun generatesLeftoverChickenQuesadilla() {
        val wrap = RecipeTemplateEngine.buildSuggestions(
            foods = listOf(
                food("Tortillas", FoodCategory.PANTRY, today.plusMonths(1)),
                food("Cooked chicken", FoodCategory.LEFTOVERS, today.plusDays(1)),
                food("Cheddar", FoodCategory.DAIRY, today.plusWeeks(2))
            ),
            today = today
        ).first { it.templateId == "wrap" }

        assertEquals("Leftover Chicken Quesadilla", wrap.title)
        assertTrue(wrap.scoreReasons.any { it.contains("leftovers", ignoreCase = true) })
        assertTrue(CookingTool.MICROWAVE in wrap.tools)
    }

    @Test
    fun templateMissingRequiredIngredientIsShownClearly() {
        val stirFry = RecipeTemplateEngine.buildSuggestions(
            foods = listOf(
                food("Chicken breast", FoodCategory.MEAT, today.plusDays(1)),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(3))
            ),
            today = today
        ).first { it.templateId == "stir_fry" }

        assertEquals(listOf("vegetable"), stirFry.missingIngredients)
        assertTrue(stirFry.scoreReasons.any { it.contains("Only missing vegetable") })
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
