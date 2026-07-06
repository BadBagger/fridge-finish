package com.fridgefinish.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ReceiptTextParserTest {
    private val today = LocalDate.of(2026, 7, 6)

    @Test
    fun extractsLikelyFoodLinesAndSkipsTotals() {
        val items = ReceiptTextParser.extractItems(
            """
            ORGANIC MILK 4.99
            BANANAS 1.25
            CHICKEN BREAST 12.42
            SUBTOTAL 18.66
            VISA APPROVED
            TAX 0.42
            """.trimIndent(),
            today
        )

        assertEquals(listOf("Organic Milk", "Bananas", "Chicken Breast"), items.map { it.name })
        assertFalse(items.any { it.name.contains("Subtotal", ignoreCase = true) })
    }

    @Test
    fun assignsUsefulDefaultCategoriesAndDates() {
        val items = ReceiptTextParser.extractItems("YOGURT 2.99\nLETTUCE 1.99\nRICE 6.49", today)

        assertEquals(FoodCategory.DAIRY, items[0].category)
        assertEquals(today.plusDays(7), items[0].expirationDate)
        assertEquals(FoodCategory.PRODUCE, items[1].category)
        assertEquals(today.plusDays(5), items[1].expirationDate)
        assertEquals(FoodCategory.PANTRY, items[2].category)
        assertEquals(today.plusMonths(6), items[2].expirationDate)
    }
}
