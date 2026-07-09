package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IngredientClassifierTest {
    private val today = LocalDate.of(2026, 7, 8)

    @Test
    fun mapsCommonFoodNamesToIngredientCategories() {
        assertEquals(IngredientCategory.PROTEIN, IngredientClassifier.classifyName("Chicken breast").category)
        assertEquals(IngredientCategory.PROTEIN, IngredientClassifier.classifyName("Eggs").category)
        assertEquals(IngredientCategory.GRAIN, IngredientClassifier.classifyName("Rice").category)
        assertEquals(IngredientCategory.GRAIN, IngredientClassifier.classifyName("Pasta").category)
        assertEquals(IngredientCategory.DAIRY, IngredientClassifier.classifyName("Cheddar").category)
        assertEquals(IngredientCategory.VEGETABLE, IngredientClassifier.classifyName("Lettuce").category)
        assertEquals(IngredientCategory.FRUIT, IngredientClassifier.classifyName("Banana").category)
        assertEquals(IngredientCategory.SAUCE, IngredientClassifier.classifyName("Soy sauce").category)
        assertEquals(IngredientCategory.CONDIMENT, IngredientClassifier.classifyName("Ranch").category)
    }

    @Test
    fun normalizesAbbreviationsAndPluralFoods() {
        assertEquals("chicken", normalizeIngredientName("CHIX"))
        assertTrue("tomatoes".containsWholeFoodTerm("tomato"))
        assertTrue("bell peppers".containsWholeFoodTerm("bell pepper"))
        assertTrue("peppers".containsWholeFoodTerm("pepper"))
    }

    @Test
    fun cookedChickenIsLeftoverAndProteinRisk() {
        val classified = IngredientClassifier.classify(
            FoodItemEntity(
                name = "Cooked chicken",
                category = FoodCategory.MEAT,
                location = FoodLocation.FRIDGE,
                openedDate = today.minusDays(3),
                expirationDate = today.plusDays(1),
                reminderDaysBefore = 2
            ),
            today
        )

        assertEquals(IngredientCategory.LEFTOVER, classified.category)
        assertTrue(classified.isLeftover)
        assertEquals(FoodSafetyRiskLevel.HIGH, classified.safetyRiskLevel)
        assertTrue(classified.priorityScore > 40)
    }
}
