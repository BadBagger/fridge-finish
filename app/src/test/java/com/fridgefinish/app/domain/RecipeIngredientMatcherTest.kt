package com.fridgefinish.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeIngredientMatcherTest {
    @Test
    fun milkDoesNotCountAsYogurtJustBecauseItIsDairy() {
        assertFalse(
            recipeIngredientMatchesFood(
                ingredientLabel = "yogurt",
                ingredientKeywords = "yogurt",
                ingredientCategory = "DAIRY",
                foodName = "Wal-Mart Stores Inc. Organic Vitamin D Milk",
                foodCategory = FoodCategory.DAIRY
            )
        )
    }

    @Test
    fun milkStillCountsForSmoothieMilkIngredient() {
        assertTrue(
            recipeIngredientMatchesFood(
                ingredientLabel = "milk or yogurt",
                ingredientKeywords = "milk,yogurt",
                ingredientCategory = "DAIRY",
                foodName = "Wal-Mart Stores Inc. Organic Vitamin D Milk",
                foodCategory = FoodCategory.DAIRY
            )
        )
    }

    @Test
    fun pierogiDoesNotCountAsSnackPlateCheese() {
        assertFalse(
            recipeIngredientMatchesFood(
                ingredientLabel = "cheese, yogurt, or hummus",
                ingredientKeywords = "cheese,yogurt,egg,hummus",
                ingredientCategory = "DAIRY",
                foodName = "Kasia's Pierogi Potato & Cheese",
                foodCategory = FoodCategory.DAIRY
            )
        )
    }

    @Test
    fun pierogiCanCountForPastaStyleRecipe() {
        assertTrue(
            recipeIngredientMatchesFood(
                ingredientLabel = "pasta",
                ingredientKeywords = "pasta,noodle,pierogi,ravioli",
                ingredientCategory = "PANTRY",
                foodName = "Kasia's Pierogi Potato & Cheese",
                foodCategory = FoodCategory.PANTRY
            )
        )
    }

    @Test
    fun produceCategoryAloneDoesNotMatchFruitIngredient() {
        assertFalse(
            recipeIngredientMatchesFood(
                ingredientLabel = "fruit",
                ingredientKeywords = "berry,banana,mango,strawberry",
                ingredientCategory = "PRODUCE",
                foodName = "Romaine Lettuce",
                foodCategory = FoodCategory.PRODUCE
            )
        )
    }
}
