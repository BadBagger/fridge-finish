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

    @Test
    fun costcoReceiptSkipsHeaderAddressAndPaymentLines() {
        val items = ReceiptTextParser.extractItems(
            """
            COSTCO WHOLESALE
            Seattle #01
            4401 4th Ave South
            Seattle, WA 98134
            24311 VAR. MUFFIN 9.99
            87745 ROTISSERIE 4.99
            11545 STREET TACOS 15.76
            990551 BASIL PESTO 9.49
            878137 18CT EGGS 12.87
            SUBTOTAL 211.27
            VISA APPROVED
            TOTAL NUMBER OF ITEMS SOLD = 16
            """.trimIndent(),
            today
        )

        assertEquals(
            listOf("Var Muffin", "Rotisserie", "Street Tacos", "Basil Pesto", "Eggs"),
            items.map { it.name }
        )
        assertFalse(items.any { it.name.contains("Seattle", ignoreCase = true) })
        assertFalse(items.any { it.name.contains("Wholesale", ignoreCase = true) })
    }

    @Test
    fun walmartReceiptKeepsFoodAndSkipsGeneralMerchandise() {
        val items = ReceiptTextParser.extractItems(
            """
            WAL-MART-SUPERSTORE
            MANAGER TOD LINGA
            888 WALL STORE ST
            HAND TOWEL 075953630184 2.97 X
            GATORADE 068949055223 2.00 X
            T-SHIRT 036231552452 16.88 X
            PUSH PINS 088348997350 1.24 X
            SUBTOTAL 23.09
            """.trimIndent(),
            today
        )

        assertEquals(listOf("Gatorade"), items.map { it.name })
    }

    @Test
    fun walmartGroceryRowsStripUpcAndTaxFlags() {
        val items = ReceiptTextParser.extractItems(
            """
            BREAD 007225003712 F 2.88 N
            GV PNT BUTTR 007874237003 F 3.84 N
            GV CHNK CHKN 007874206784 F 1.98 X
            FOLGERS 002550000377 F 10.48 N
            EGGS 060538871459 F 1.88 O
            TOTAL 46.30
            """.trimIndent(),
            today
        )

        assertEquals(
            listOf("Bread", "Gv Peanut Butter", "Gv Chunk Chicken", "Folgers", "Eggs"),
            items.map { it.name }
        )
    }

    @Test
    fun produceReceiptSkipsSpecialLinesAndKeepsProduce() {
        val items = ReceiptTextParser.extractItems(
            """
            ZUCHINNI GREEN $4.66
            0.778kg NET @ $5.99/kg
            BANANA CAVENDISH $1.32
            SPECIAL $0.99
            POTATOES BRUSHED $3.97
            BROCCOLI $4.84
            BRUSSEL SPROUTS $5.15
            GRAPES GREEN $7.03
            PEAS SNOW $3.27
            TOMATOES GRAPE $2.99
            LETTUCE ICEBERG $2.49
            SUBTOTAL $39.20
            """.trimIndent(),
            today
        )

        assertEquals(
            listOf(
                "Zuchinni Green",
                "Banana Cavendish",
                "Potatoes Brushed",
                "Broccoli",
                "Brussel Sprouts",
                "Grapes Green",
                "Peas Snow",
                "Tomatoes Grape",
                "Lettuce Iceberg"
            ),
            items.map { it.name }
        )
    }
}
