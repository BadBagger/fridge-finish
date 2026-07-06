package com.fridgefinish.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingLabelFormatterTest {
    @Test
    fun genericProduceBecomesSpecificSuggestion() {
        assertEquals("Spinach, peppers, or carrots", "produce".cleanShoppingName())
        assertEquals("Spinach, peppers, or carrots", "vegetables".cleanShoppingName())
    }

    @Test
    fun genericCategoriesBecomeUsefulShoppingSuggestions() {
        assertEquals("Milk, yogurt, or cheese", "dairy".cleanShoppingName())
        assertEquals("Rice, pasta, or beans", "pantry".cleanShoppingName())
        assertEquals("Crackers, pretzels, or nuts", "snacks".cleanShoppingName())
    }

    @Test
    fun preservesSpecificIngredientText() {
        assertEquals("Greek yogurt", "greek yogurt".cleanShoppingName())
        assertEquals("Spinach or peppers", "  spinach   or   peppers  ".cleanShoppingName())
    }
}
