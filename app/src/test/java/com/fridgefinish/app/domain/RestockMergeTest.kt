package com.fridgefinish.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RestockMergeTest {
    @Test
    fun skipsExactOpenShopDuplicates() {
        val result = missingItemsNotAlreadyInShop(
            missingItems = listOf("Milk, yogurt, or cheese", "Rice, pasta, or beans"),
            openShopItems = listOf("milk, yogurt, or cheese")
        )

        assertEquals(listOf("Rice, pasta, or beans"), result.itemsToAdd)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun skipsVagueLabelsThatCleanToSameSuggestion() {
        val result = missingItemsNotAlreadyInShop(
            missingItems = listOf("produce", "snacks"),
            openShopItems = listOf("Spinach, peppers, or carrots")
        )

        assertEquals(listOf("snacks"), result.itemsToAdd)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun ignoresPurchasedItemsWhenCallerExcludesThem() {
        val result = missingItemsNotAlreadyInShop(
            missingItems = listOf("produce"),
            openShopItems = emptyList()
        )

        assertEquals(listOf("produce"), result.itemsToAdd)
        assertEquals(0, result.skippedCount)
    }
}
