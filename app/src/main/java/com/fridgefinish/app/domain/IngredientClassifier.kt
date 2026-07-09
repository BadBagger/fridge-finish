package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import java.time.LocalDate

data class ClassifiedIngredient(
    val normalizedName: String,
    val category: IngredientCategory,
    val subcategory: String?,
    val isOpened: Boolean,
    val isLeftover: Boolean,
    val priorityScore: Int,
    val safetyRiskLevel: FoodSafetyRiskLevel
)

object IngredientClassifier {
    fun classify(food: FoodItemEntity, today: LocalDate = LocalDate.now()): ClassifiedIngredient {
        val base = classifyName(food.name, food.category, food.location)
        val leftover = base.isLeftover || food.isLeftover || food.category == FoodCategory.LEFTOVERS
        val opened = food.isOpened || food.openedDate != null
        val status = FreshnessCalculator.status(food.expirationDate, food.reminderDaysBefore, food.isFinished, today)
        val risk = safetyRisk(food, base.category, leftover, opened, status, today)
        val urgency = when (status) {
            FreshnessStatus.EXPIRED -> 40
            FreshnessStatus.EXPIRES_TODAY -> 35
            FreshnessStatus.EAT_SOON -> 24
            FreshnessStatus.FRESH -> 8
            FreshnessStatus.FINISHED -> 0
        }
        val riskScore = when (risk) {
            FoodSafetyRiskLevel.HIGH -> 24
            FoodSafetyRiskLevel.MEDIUM -> 12
            FoodSafetyRiskLevel.LOW -> 0
        }
        val openedScore = if (opened) 6 else 0
        val leftoverScore = if (leftover) 10 else 0

        return base.copy(
            isOpened = opened,
            isLeftover = leftover,
            priorityScore = (urgency + riskScore + openedScore + leftoverScore).coerceIn(0, 100),
            safetyRiskLevel = risk
        )
    }

    fun classifyName(
        rawName: String,
        fallbackCategory: FoodCategory = FoodCategory.OTHER,
        location: FoodLocation = FoodLocation.FRIDGE
    ): ClassifiedIngredient {
        val normalized = normalizeIngredientName(rawName)
        val tokens = normalized.split(" ").filter { it.isNotBlank() }.toSet()
        val leftover = tokens.any { it in leftoverTerms } || normalized.containsWholeFoodTerm("leftover")
        val frozen = location == FoodLocation.FREEZER || fallbackCategory == FoodCategory.FROZEN || tokens.any { it == "frozen" }

        val detected = when {
            leftover -> IngredientCategory.LEFTOVER
            frozen -> IngredientCategory.FROZEN
            anyTerm(normalized, proteinTerms) || fallbackCategory == FoodCategory.MEAT -> IngredientCategory.PROTEIN
            anyTerm(normalized, dairyTerms) || fallbackCategory == FoodCategory.DAIRY -> IngredientCategory.DAIRY
            anyTerm(normalized, vegetableTerms) -> IngredientCategory.VEGETABLE
            anyTerm(normalized, fruitTerms) -> IngredientCategory.FRUIT
            anyTerm(normalized, grainTerms) || fallbackCategory == FoodCategory.PANTRY -> IngredientCategory.GRAIN
            anyTerm(normalized, sauceTerms) -> IngredientCategory.SAUCE
            anyTerm(normalized, condimentTerms) || fallbackCategory == FoodCategory.CONDIMENTS -> IngredientCategory.CONDIMENT
            anyTerm(normalized, spiceTerms) -> IngredientCategory.SPICE
            anyTerm(normalized, cannedTerms) -> IngredientCategory.CANNED
            anyTerm(normalized, bakingTerms) -> IngredientCategory.BAKING
            anyTerm(normalized, drinkTerms) || fallbackCategory == FoodCategory.DRINKS -> IngredientCategory.DRINK
            fallbackCategory == FoodCategory.SNACKS || anyTerm(normalized, snackTerms) -> IngredientCategory.SNACK
            fallbackCategory == FoodCategory.PRODUCE -> IngredientCategory.VEGETABLE
            else -> IngredientCategory.OTHER
        }

        return ClassifiedIngredient(
            normalizedName = normalized,
            category = detected,
            subcategory = subcategoryFor(normalized, detected),
            isOpened = false,
            isLeftover = leftover,
            priorityScore = 0,
            safetyRiskLevel = FoodSafetyRiskLevel.LOW
        )
    }

    fun categoryFromRecipeCategory(raw: String?): Set<IngredientCategory> {
        val value = raw?.trim()?.uppercase().orEmpty()
        return when (value) {
            "PROTEIN", "MEAT" -> setOf(IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER)
            "VEGETABLE", "VEGETABLES" -> setOf(IngredientCategory.VEGETABLE)
            "FRUIT", "FRUITS" -> setOf(IngredientCategory.FRUIT)
            "PRODUCE" -> setOf(IngredientCategory.VEGETABLE, IngredientCategory.FRUIT)
            "GRAIN", "GRAINS", "PANTRY" -> setOf(IngredientCategory.GRAIN, IngredientCategory.CANNED, IngredientCategory.BAKING)
            "DAIRY" -> setOf(IngredientCategory.DAIRY)
            "SAUCE" -> setOf(IngredientCategory.SAUCE)
            "CONDIMENT", "CONDIMENTS" -> setOf(IngredientCategory.CONDIMENT, IngredientCategory.SAUCE)
            "SPICE", "SPICES" -> setOf(IngredientCategory.SPICE)
            "SNACK", "SNACKS" -> setOf(IngredientCategory.SNACK)
            "LEFTOVER", "LEFTOVERS" -> setOf(IngredientCategory.LEFTOVER)
            "FROZEN" -> setOf(IngredientCategory.FROZEN)
            "CANNED" -> setOf(IngredientCategory.CANNED)
            "BAKING" -> setOf(IngredientCategory.BAKING)
            "DRINK", "DRINKS" -> setOf(IngredientCategory.DRINK)
            else -> emptySet()
        }
    }

    private fun safetyRisk(
        food: FoodItemEntity,
        ingredientCategory: IngredientCategory,
        isLeftover: Boolean,
        isOpened: Boolean,
        status: FreshnessStatus,
        today: LocalDate
    ): FoodSafetyRiskLevel {
        if (status == FreshnessStatus.EXPIRED) return FoodSafetyRiskLevel.HIGH
        if (isLeftover) return FoodSafetyRiskLevel.HIGH
        if (ingredientCategory in highRiskCategories) {
            val openDays = food.openedDate?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today) } ?: 0
            return when {
                isOpened && openDays >= 3 -> FoodSafetyRiskLevel.HIGH
                isOpened || status in setOf(FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON) -> FoodSafetyRiskLevel.MEDIUM
                else -> FoodSafetyRiskLevel.LOW
            }
        }
        return if (status == FreshnessStatus.EXPIRES_TODAY) FoodSafetyRiskLevel.MEDIUM else FoodSafetyRiskLevel.LOW
    }

    private fun anyTerm(normalized: String, terms: Set<String>): Boolean =
        terms.any { normalized.containsWholeFoodTerm(it) }

    private fun subcategoryFor(normalized: String, category: IngredientCategory): String? =
        when (category) {
            IngredientCategory.PROTEIN -> firstMatching(normalized, proteinTerms)
            IngredientCategory.VEGETABLE -> firstMatching(normalized, vegetableTerms)
            IngredientCategory.FRUIT -> firstMatching(normalized, fruitTerms)
            IngredientCategory.GRAIN -> firstMatching(normalized, grainTerms)
            IngredientCategory.DAIRY -> firstMatching(normalized, dairyTerms)
            IngredientCategory.SAUCE -> firstMatching(normalized, sauceTerms)
            IngredientCategory.CONDIMENT -> firstMatching(normalized, condimentTerms)
            IngredientCategory.SPICE -> firstMatching(normalized, spiceTerms)
            IngredientCategory.SNACK -> firstMatching(normalized, snackTerms)
            IngredientCategory.CANNED -> firstMatching(normalized, cannedTerms)
            IngredientCategory.BAKING -> firstMatching(normalized, bakingTerms)
            IngredientCategory.DRINK -> firstMatching(normalized, drinkTerms)
            IngredientCategory.LEFTOVER -> firstMatching(normalized, proteinTerms + vegetableTerms + grainTerms) ?: "leftover"
            IngredientCategory.FROZEN -> "frozen"
            IngredientCategory.OTHER -> null
        }

    private fun firstMatching(normalized: String, terms: Set<String>): String? =
        terms.firstOrNull { normalized.containsWholeFoodTerm(it) }

    private val highRiskCategories = setOf(
        IngredientCategory.PROTEIN,
        IngredientCategory.DAIRY,
        IngredientCategory.LEFTOVER
    )

    private val leftoverTerms = setOf("leftover", "cooked", "prepared", "takeout")
    private val proteinTerms = setOf("chicken", "beef", "pork", "turkey", "fish", "salmon", "tuna", "shrimp", "egg", "tofu", "tempeh", "bean", "lentil", "meat", "ham", "bacon", "sausage")
    private val vegetableTerms = setOf("lettuce", "tomato", "pepper", "bell pepper", "onion", "spinach", "greens", "broccoli", "carrot", "celery", "zucchini", "potato", "pea", "corn", "cucumber", "mushroom", "brussel sprout", "vegetable")
    private val fruitTerms = setOf("banana", "apple", "berry", "strawberry", "blueberry", "raspberry", "grape", "mango", "orange", "lemon", "lime", "fruit")
    private val grainTerms = setOf("rice", "pasta", "noodle", "bread", "tortilla", "wrap", "oat", "quinoa", "cereal", "cracker", "flour", "grain")
    private val dairyTerms = setOf("milk", "cheese", "cheddar", "yogurt", "cream", "butter", "sour cream", "cottage cheese")
    private val sauceTerms = setOf("sauce", "soy sauce", "pasta sauce", "salsa", "marinara", "teriyaki", "broth", "stock")
    private val condimentTerms = setOf("ranch", "mayo", "mayonnaise", "mustard", "ketchup", "dressing", "hummus", "dip", "relish")
    private val spiceTerms = setOf("salt", "pepper", "garlic powder", "paprika", "cumin", "oregano", "basil", "seasoning", "spice")
    private val snackTerms = setOf("chip", "pretzel", "cracker", "cookie", "granola", "nuts", "snack")
    private val cannedTerms = setOf("canned", "can", "beans", "tomatoes", "soup")
    private val bakingTerms = setOf("flour", "sugar", "baking", "yeast", "cocoa")
    private val drinkTerms = setOf("juice", "soda", "water", "tea", "coffee", "drink", "milk")
}

fun normalizeIngredientName(raw: String): String {
    val base = raw
        .lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { normalizationAliases[it] ?: it }

    return phraseAliases.entries.fold(base) { value, alias ->
        value.replace(Regex("(^|\\s)${Regex.escape(alias.key)}(\\s|$)")) { match ->
            "${match.groupValues[1]}${alias.value}${match.groupValues[2]}"
        }
    }.trim()
}

fun String.containsWholeFoodTerm(term: String): Boolean {
    val normalized = normalizeIngredientName(this)
    return foodTermVariants(term).any { variant ->
        val pattern = Regex("(^|\\s)${Regex.escape(variant)}(\\s|$)")
        pattern.containsMatchIn(normalized)
    }
}

fun foodTermVariants(term: String): Set<String> {
    val clean = normalizeIngredientName(term)
    if (clean.isBlank()) return emptySet()

    val variants = mutableSetOf(clean)
    variants += when {
        clean.endsWith("y") -> clean.dropLast(1) + "ies"
        clean.endsWith("s") -> clean.dropLast(1)
        else -> clean + "s"
    }
    if (clean.endsWith("es")) variants += clean.dropLast(2)
    if (clean.endsWith("ies")) variants += clean.dropLast(3) + "y"
    return variants
}

private val normalizationAliases = mapOf(
    "chix" to "chicken",
    "chx" to "chicken",
    "tomatoes" to "tomato",
    "peppers" to "pepper",
    "eggs" to "egg",
    "berries" to "berry",
    "tortillas" to "tortilla",
    "noodles" to "noodle",
    "veggies" to "vegetable",
    "veg" to "vegetable"
)

private val phraseAliases = mapOf(
    "bell peppers" to "bell pepper",
    "bell pepp" to "bell pepper"
)
