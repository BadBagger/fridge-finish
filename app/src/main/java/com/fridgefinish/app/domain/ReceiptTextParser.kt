package com.fridgefinish.app.domain

import java.time.LocalDate

data class ReceiptImportCandidate(
    val name: String,
    val category: FoodCategory,
    val expirationDate: LocalDate,
    val reminderDaysBefore: Int
)

object ReceiptTextParser {
    fun extractItems(rawText: String, today: LocalDate = LocalDate.now()): List<ReceiptImportCandidate> {
        return rawText
            .lineSequence()
            .mapNotNull { parseLine(it, today) }
            .distinctBy { it.name.lowercase() }
            .take(40)
            .toList()
    }

    private fun parseLine(line: String, today: LocalDate): ReceiptImportCandidate? {
        val cleaned = line
            .replace(Regex("""\b\d{10,}\b"""), " ")
            .replace(Regex("""[$]\s*\d+([.,]\d{2})?"""), " ")
            .replace(Regex("""\b\d+[.,]\d{2}\b"""), " ")
            .replace(Regex("""\b\d+\s*(ea|lb|lbs|oz|ct|pk|x)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[^A-Za-z &'\-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleaned.length < 3 || cleaned.length > 48) return null
        val lower = cleaned.lowercase()
        if (ignoredTerms.any { lower.contains(it) }) return null
        if (lower.count { it.isLetter() } < 3) return null

        val name = cleaned
            .split(" ")
            .filterNot { it.length == 1 && !singleLetterWords.contains(it.lowercase()) }
            .joinToString(" ")
            .trim()
            .toTitleCase()

        if (name.length < 3) return null
        val category = inferCategory(name)
        return ReceiptImportCandidate(
            name = name,
            category = category,
            expirationDate = defaultExpirationDate(category, today),
            reminderDaysBefore = FreshnessCalculator.defaultReminderDays(category)
        )
    }

    private fun inferCategory(name: String): FoodCategory {
        val text = name.lowercase()
        return when {
            listOf("chicken", "beef", "pork", "turkey", "salmon", "fish", "meat", "bacon", "sausage").any { text.contains(it) } -> FoodCategory.MEAT
            listOf("milk", "cheese", "yogurt", "butter", "cream", "eggs", "egg").any { text.contains(it) } -> FoodCategory.DAIRY
            listOf("lettuce", "apple", "banana", "berry", "berries", "tomato", "pepper", "onion", "carrot", "spinach", "broccoli", "avocado", "grape", "lime", "lemon").any { text.contains(it) } -> FoodCategory.PRODUCE
            listOf("frozen", "ice cream").any { text.contains(it) } -> FoodCategory.FROZEN
            listOf("juice", "soda", "water", "tea", "coffee", "drink", "beverage").any { text.contains(it) } -> FoodCategory.DRINKS
            listOf("chips", "crackers", "pretzel", "cookies", "snack", "nuts", "popcorn").any { text.contains(it) } -> FoodCategory.SNACKS
            listOf("ketchup", "mustard", "sauce", "dressing", "mayo", "salsa", "condiment").any { text.contains(it) } -> FoodCategory.CONDIMENTS
            listOf("rice", "pasta", "bread", "cereal", "flour", "sugar", "beans", "tortilla", "oats").any { text.contains(it) } -> FoodCategory.PANTRY
            else -> FoodCategory.OTHER
        }
    }

    private fun defaultExpirationDate(category: FoodCategory, today: LocalDate): LocalDate =
        when (category) {
            FoodCategory.LEFTOVERS -> today.plusDays(3)
            FoodCategory.MEAT -> today.plusDays(2)
            FoodCategory.DAIRY -> today.plusDays(7)
            FoodCategory.PRODUCE -> today.plusDays(5)
            FoodCategory.FROZEN -> today.plusMonths(3)
            FoodCategory.PANTRY -> today.plusMonths(6)
            else -> today.plusDays(7)
        }

    private val ignoredTerms = setOf(
        "subtotal",
        "total",
        "balance",
        "change",
        "cash",
        "credit",
        "debit",
        "visa",
        "mastercard",
        "approved",
        "transaction",
        "receipt",
        "thank you",
        "member",
        "savings",
        "coupon",
        "tax",
        "store",
        "cashier",
        "card",
        "auth",
        "terminal"
    )

    private val singleLetterWords = setOf("a")
}

private fun String.toTitleCase(): String =
    split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
