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
        val lineItems = rawText
            .lineSequence()
            .mapNotNull { parseLine(it, today) }
            .toList()

        val hasLongOcrLine = rawText.lines().any { it.trim().length > 96 }
        if (lineItems.isNotEmpty() && !hasLongOcrLine && rawText.lines().count { it.trim().length > 2 } > 1) {
            return lineItems
                .distinctBy { it.name.lowercase() }
                .take(40)
        }

        return (lineItems + parseFlattenedReceipt(rawText, today))
            .distinctBy { it.name.lowercase() }
            .take(40)
            .toList()
    }

    private fun parseLine(line: String, today: LocalDate): ReceiptImportCandidate? {
        val raw = line.trim()
        if (raw.length > 96) return null
        if (Regex("""\bnet\s*@""", RegexOption.IGNORE_CASE).containsMatchIn(raw)) return null
        val hasPrice = Regex("""(?:^|\s)\$?\d+[.,]\d{2}(?:\s|$)""").containsMatchIn(raw)
        if (Regex("""(?:^|\s)\$?\d+[.,]\d{2}(?:\s|$)""").findAll(raw).take(2).count() > 1) return null
        val hasWarehouseItemShape = Regex("""^\s*\d{4,}\s+[A-Za-z].*\s+\d+[.,]\d{2}\s*$""").containsMatchIn(raw)
        if (!hasPrice && !hasWarehouseItemShape) return null

        return candidateFromCleanedName(cleanReceiptName(line), today)
    }

    private fun parseFlattenedReceipt(rawText: String, today: LocalDate): List<ReceiptImportCandidate> {
        val normalized = rawText
            .replace(Regex("""[\r\n\t]+"""), " ")
            .replace(Regex("""[|*_]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (normalized.length < 6) return emptyList()

        val candidates = mutableListOf<ReceiptImportCandidate>()

        parseFlattenedUpcRows(normalized, today).forEach(candidates::add)
        parseFlattenedWarehouseRows(normalized, today).forEach(candidates::add)
        parseFlattenedPriceRows(normalized, today).forEach(candidates::add)

        return candidates
    }

    private fun parseFlattenedUpcRows(text: String, today: LocalDate): List<ReceiptImportCandidate> {
        val candidates = mutableListOf<ReceiptImportCandidate>()
        val upcMatches = Regex("""\b\d{10,14}\b""").findAll(text).toList()
        var previousEnd = 0

        upcMatches.forEach { match ->
            val segment = text.substring(previousEnd, match.range.first)
            val nameText = segment
                .replace(Regex("""\b\d+[.,]\d{2}\s*[A-Za-z]?\s*$"""), " ")
                .replace(Regex("""\b[A-Za-z]\s*$"""), " ")
                .substringAfterLastKnownBoundary()

            candidateFromCleanedName(cleanReceiptName(nameText), today)?.let(candidates::add)
            previousEnd = match.range.last + 1
        }

        return candidates
    }

    private fun parseFlattenedWarehouseRows(text: String, today: LocalDate): List<ReceiptImportCandidate> {
        val candidates = mutableListOf<ReceiptImportCandidate>()
        val itemNumbers = Regex("""\b\d{4,7}\b""")
            .findAll(text)
            .filter { match ->
                val after = text.drop(match.range.last + 1).trimStart()
                after.firstOrNull()?.isLetter() == true || Regex("""^\d+\s*[A-Za-z]""").containsMatchIn(after)
            }
            .toList()

        itemNumbers.forEachIndexed { index, match ->
            val nextStart = itemNumbers.getOrNull(index + 1)?.range?.first ?: text.length
            val segment = text.substring(match.range.last + 1, nextStart)
                .substringBeforePaymentBoundary()

            candidateFromCleanedName(cleanReceiptName(segment), today)?.let(candidates::add)
        }

        return candidates
    }

    private fun parseFlattenedPriceRows(text: String, today: LocalDate): List<ReceiptImportCandidate> {
        val candidates = mutableListOf<ReceiptImportCandidate>()
        var previousEnd = 0

        // Produce receipts may flatten into repeated ITEM NAME PRICE chunks.
        Regex("""\$?\d+[.,]\d{2}\b""")
            .findAll(text)
            .forEach { match ->
                val segment = text.substring(previousEnd, match.range.first)
                    .substringAfterLastKnownBoundary()
                    .replace(Regex("""\b\d+[.,]\d+\s*(kg|g|lb|lbs|oz)\s+net\s*@?\s*$""", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("""\bnet\s*@?\s*$""", RegexOption.IGNORE_CASE), " ")

                candidateFromCleanedName(cleanReceiptName(segment), today)?.let(candidates::add)
                previousEnd = consumeUnitSuffix(text, match.range.last + 1)
            }

        return candidates
    }

    private fun consumeUnitSuffix(text: String, startIndex: Int): Int {
        val suffix = Regex("""^\s*/?\s*(kg|g|lb|lbs|oz)\b\s*""", RegexOption.IGNORE_CASE)
            .find(text.substring(startIndex))
            ?: return startIndex
        return startIndex + suffix.range.last + 1
    }

    private fun cleanReceiptName(text: String): String =
        text
            .replace(Regex("""\b\d{10,}\b"""), " ")
            .replace(Regex("""^\s*\d{4,}\s+"""), " ")
            .replace(Regex("""[$]\s*\d+([.,]\d{2})?"""), " ")
            .replace(Regex("""\b\d+[.,]\d{2}\b"""), " ")
            .replace(Regex("""\b\d+[.,]\d+\s*(kg|g|lb|lbs|oz)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\bnet\s*@\s*[$]?\d+[.,]\d{2}/?(kg|lb|lbs)?\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b\d+\s*(ea|lb|lbs|oz|ct|pk|x)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[^A-Za-z &'\-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun candidateFromCleanedName(cleaned: String, today: LocalDate): ReceiptImportCandidate? {
        if (cleaned.length < 3 || cleaned.length > 48) return null
        val lower = cleaned.lowercase()
        if (ignoredTerms.any { lower.contains(it) }) return null
        if (addressTerms.any { lower.contains(it) }) return null
        if (nonFoodTerms.any { lower.contains(it) }) return null
        if (lower.count { it.isLetter() } < 3) return null

        val name = cleaned
            .split(" ")
            .filterNot { it.length == 1 && !singleLetterWords.contains(it.lowercase()) }
            .joinToString(" ")
            .trim()
            .expandReceiptAbbreviations()
            .toTitleCase()

        if (name.length < 3) return null
        if (name.split(" ").size == 1 && !singleWordFoodTerms.any { name.contains(it, ignoreCase = true) }) return null
        val category = inferCategory(name)
        if (category == FoodCategory.OTHER && !unknownButLikelyFoodTerms.any { name.contains(it, ignoreCase = true) }) return null
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
            listOf("chicken", "chkn", "beef", "pork", "turkey", "salmon", "fish", "meat", "bacon", "sausage").any { text.contains(it) } -> FoodCategory.MEAT
            listOf("milk", "cheese", "yogurt", "cream", "eggs", "egg").any { text.contains(it) } -> FoodCategory.DAIRY
            listOf("lettuce", "apple", "banana", "berry", "berries", "tomato", "pepper", "onion", "carrot", "spinach", "broccoli", "avocado", "grape", "grapes", "lime", "lemon", "zucchini", "zuchinni", "potato", "potatoes", "brussel", "sprout", "peas").any { text.contains(it) } -> FoodCategory.PRODUCE
            listOf("frozen", "ice cream").any { text.contains(it) } -> FoodCategory.FROZEN
            listOf("gatorade", "juice", "soda", "water", "tea", "coffee", "folgers", "drink", "beverage").any { text.contains(it) } -> FoodCategory.DRINKS
            listOf("chips", "crackers", "pretzel", "cookies", "snack", "nuts", "popcorn").any { text.contains(it) } -> FoodCategory.SNACKS
            listOf("ketchup", "mustard", "sauce", "dressing", "mayo", "salsa", "condiment").any { text.contains(it) } -> FoodCategory.CONDIMENTS
            listOf("butter", "buttr", "rice", "pasta", "bread", "cereal", "flour", "sugar", "beans", "tortilla", "oats").any { text.contains(it) } -> FoodCategory.PANTRY
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
        "terminal",
        "wholesale",
        "basket",
        "items sold",
        "purchase",
        "amount",
        "loyalty",
        "special"
    )

    private val nonFoodTerms = setOf(
        "bath",
        "holiday",
        "pins",
        "push pin",
        "shirt",
        "t-shirt",
        "towel",
        "washcloth"
    )

    private val addressTerms = setOf(
        " ave",
        " avenue",
        " st",
        " street",
        " rd",
        " road",
        " blvd",
        " drive",
        " dr",
        " lane",
        " ln",
        " seattle",
        " wa "
    )

    private val singleWordFoodTerms = setOf(
        "apple",
        "apples",
        "banana",
        "bananas",
        "berries",
        "bread",
        "broccoli",
        "butter",
        "cheese",
        "chicken",
        "cookies",
        "eggs",
        "folgers",
        "gatorade",
        "grapes",
        "lettuce",
        "muffin",
        "muffins",
        "milk",
        "pesto",
        "rice",
        "rotisserie",
        "tacos",
        "yogurt",
        "zuchinni",
        "zucchini"
    )

    private val unknownButLikelyFoodTerms = setOf(
        "muffin",
        "rotisserie",
        "taco",
        "tacos",
        "pesto",
        "trail mix",
        "artisan roll"
    )

    private val singleLetterWords = setOf("a")
}

private fun String.substringAfterLastKnownBoundary(): String {
    val boundaries = listOf(
        " subtotal ",
        " total ",
        " tax ",
        " date ",
        " manager ",
        " member ",
        " store ",
        " superstore ",
        " wholesale ",
        " x ",
        " n ",
        " o ",
        " f "
    )
    val lower = " ${lowercase()} "
    val boundary = boundaries
        .map { lower.lastIndexOf(it) to it.length }
        .filter { it.first >= 0 }
        .maxByOrNull { it.first }
        ?: return this
    return drop((boundary.first + boundary.second - 1).coerceAtMost(length))
}

private fun String.substringBeforePaymentBoundary(): String =
    split(Regex("""\b(?:BOTTOM|SUBTOTAL|TOTAL|TAX|VISA|CREDIT|DEBIT|CASH|CHANGE|APPROVED)\b""", RegexOption.IGNORE_CASE))
        .firstOrNull()
        .orEmpty()

private fun String.expandReceiptAbbreviations(): String =
    split(" ")
        .joinToString(" ") { word ->
            when (word.lowercase()) {
                "buttr" -> "butter"
                "chkn" -> "chicken"
                "chnk" -> "chunk"
                "nutril" -> "nutrition"
                "pnt" -> "peanut"
                else -> word
            }
        }

private fun String.toTitleCase(): String =
    split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
