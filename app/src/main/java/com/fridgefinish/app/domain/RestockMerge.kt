package com.fridgefinish.app.domain

data class RestockMergeResult(
    val itemsToAdd: List<String>,
    val skippedCount: Int
)

fun missingItemsNotAlreadyInShop(
    missingItems: List<String>,
    openShopItems: List<String>
): RestockMergeResult {
    val existing = openShopItems
        .map { it.cleanShoppingName().normalizedShoppingKey() }
        .toSet()

    val toAdd = missingItems.filterNot { missing ->
        missing.cleanShoppingName().normalizedShoppingKey() in existing
    }

    return RestockMergeResult(
        itemsToAdd = toAdd,
        skippedCount = missingItems.size - toAdd.size
    )
}

private fun String.normalizedShoppingKey(): String =
    trim().replace(Regex("\\s+"), " ").lowercase()
