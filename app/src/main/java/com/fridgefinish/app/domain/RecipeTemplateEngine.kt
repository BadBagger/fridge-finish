package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TemplateRecipeSuggestion(
    val templateId: String,
    val title: String,
    val usedFoods: List<FoodItemEntity>,
    val expiringSoonFoods: List<FoodItemEntity>,
    val missingIngredients: List<String>,
    val optionalMatchedFoods: List<FoodItemEntity>,
    val steps: List<String>,
    val minutes: Int,
    val difficulty: String,
    val reason: String,
    val tools: Set<CookingTool>,
    val filters: Set<RecipeFilter>,
    val matchScore: Int,
    val scoreReasons: List<String>
)

object RecipeTemplateEngine {
    fun buildSuggestions(
        foods: List<FoodItemEntity>,
        preferences: RecipeSuggestionPreferences = RecipeSuggestionPreferences(),
        today: LocalDate = LocalDate.now()
    ): List<TemplateRecipeSuggestion> {
        val activeFoods = foods.filter { it.isRecipeEligible(preferences, today) }
        if (activeFoods.isEmpty()) return emptyList()
        val classified = activeFoods.associateWith { IngredientClassifier.classify(it, today) }

        return templates.mapNotNull { template ->
            if (!template.matchesPreferences(preferences)) return@mapNotNull null
            val used = mutableListOf<FoodItemEntity>()
            val missing = mutableListOf<String>()
            val optional = mutableListOf<FoodItemEntity>()

            template.slots.forEach { slot ->
                val match = activeFoods
                    .filterNot { used.any { usedFood -> usedFood.id == it.id } }
                    .sortedByDescending { classified.getValue(it).priorityScore }
                    .firstOrNull { food -> slot.matches(food, classified.getValue(food)) }
                when {
                    match != null && slot.required -> used += match
                    match != null -> {
                        used += match
                        optional += match
                    }
                    slot.required && !(preferences.assumePantryStaples && slot.isPantryStaple()) -> missing += slot.shoppingLabel
                }
            }

            if (used.isEmpty()) return@mapNotNull null
            if (missing.size > 2) return@mapNotNull null
            val searchableText = template.searchableText(used, missing)
            if (allergenViolations(searchableText, preferences.allergensToAvoid).isNotEmpty()) return@mapNotNull null

            val expiringSoon = used.filter { food ->
                FreshnessCalculator.status(food.expirationDate, food.reminderDaysBefore, food.isFinished, today) in urgentStatuses
            }
            val title = template.title(used, classified)
            val score = templateScore(template, used, missing, expiringSoon, optional, preferences, today)
            TemplateRecipeSuggestion(
                templateId = template.id,
                title = title,
                usedFoods = used.distinctBy { it.id },
                expiringSoonFoods = expiringSoon.distinctBy { it.id },
                missingIngredients = missing.distinct(),
                optionalMatchedFoods = optional.distinctBy { it.id },
                steps = template.steps(used, missing),
                minutes = template.minutes,
                difficulty = if (template.minutes <= 10) "Easy" else "Moderate",
                reason = template.reason(used, expiringSoon, missing),
                tools = template.tools,
                filters = template.filters,
                matchScore = score.value,
                scoreReasons = score.reasons
            )
        }.sortedWith(
            compareByDescending<TemplateRecipeSuggestion> { it.matchScore }
                .thenBy { it.missingIngredients.size }
                .thenByDescending { it.expiringSoonFoods.size }
                .thenBy { it.minutes }
        )
    }

    private fun RecipeTemplate.matchesPreferences(preferences: RecipeSuggestionPreferences): Boolean {
        if (preferences.filters.isNotEmpty() && preferences.filters.intersect(filters).isEmpty()) return false
        if (preferences.maxMinutes != null && minutes > preferences.maxMinutes) return false
        if (preferences.tools.isNotEmpty() && preferences.tools.intersect(tools).isEmpty()) return false
        return true
    }

    private fun templateScore(
        template: RecipeTemplate,
        used: List<FoodItemEntity>,
        missing: List<String>,
        expiringSoon: List<FoodItemEntity>,
        optional: List<FoodItemEntity>,
        preferences: RecipeSuggestionPreferences,
        today: LocalDate
    ): TemplateScore {
        val reasons = mutableListOf<String>()
        var score = 28
        val todayOrTomorrow = used.filter { ChronoUnit.DAYS.between(today, it.expirationDate) in 0..1 }
        if (todayOrTomorrow.isNotEmpty()) {
            score += 25
            reasons += "Saves ${todayOrTomorrow.first().name.cleanShoppingName()} ${expiryPhrase(todayOrTomorrow.first(), today)}"
        }
        if (expiringSoon.size >= 2) {
            score += 15
            reasons += "Uses ${expiringSoon.size} ingredients that need attention"
        }
        if (used.size >= template.requiredCount) {
            score += 20
            reasons += "Uses ${used.size} ingredient${if (used.size == 1) "" else "s"} you already have"
        }
        if (used.any { IngredientClassifier.classify(it, today).isLeftover }) {
            score += 10
            reasons += "Uses leftovers"
        }
        if (preferences.filters.isNotEmpty() && preferences.filters.intersect(template.filters).isNotEmpty()) {
            score += 10
            reasons += "Matches selected meal type or preference"
        }
        if (preferences.maxMinutes != null && template.minutes <= preferences.maxMinutes) {
            score += 10
            reasons += "Fits your selected cooking time"
        }
        if (optional.isNotEmpty()) {
            score += (optional.size * 4).coerceAtMost(10)
            reasons += "Uses optional ingredients you already have"
        }
        val searchableText = template.searchableText(used, missing)
        val favoriteHits = termHits(searchableText, preferences.favoriteIngredients)
        if (favoriteHits.isNotEmpty()) {
            score += (favoriteHits.size * 8).coerceAtMost(16)
            reasons += "Uses ingredients you like"
        }
        val avoidedHits = termHits(searchableText, preferences.blockedIngredients)
        if (avoidedHits.isNotEmpty()) {
            score -= (avoidedHits.size * 12).coerceAtMost(30)
            reasons += "Includes avoided or blocked ingredient: ${avoidedHits.first()}"
        }
        val dietFit = dietaryStyleFit(searchableText, preferences.dietaryStyle, used.map { IngredientClassifier.classify(it, today) })
        score += dietFit.score
        reasons += dietFit.reasons
        val spiceFit = spiceLevelFit(searchableText, preferences.spiceLevel)
        score += spiceFit.score
        reasons += spiceFit.reasons
        val confidenceFit = template.confidenceFit(preferences.cookingConfidence)
        score += confidenceFit.score
        reasons += confidenceFit.reasons
        if (missing.isNotEmpty()) {
            score -= missing.size * 10
            reasons += if (missing.size == 1) "Only missing ${missing.first()}" else "Missing ${missing.size} major ingredients"
        } else if (preferences.assumePantryStaples && template.slots.any { it.isPantryStaple() }) {
            reasons += "Pantry basics assumed"
        }
        val blocked = preferences.blockedIngredients.map { normalizeIngredientName(it) }.filter { it.isNotBlank() }
        val blockedHit = blocked.firstOrNull { blockedTerm ->
            used.any { normalizeIngredientName(it.name).containsWholeFoodTerm(blockedTerm) } ||
                template.slots.any { normalizeIngredientName(it.shoppingLabel).containsWholeFoodTerm(blockedTerm) }
        }
        if (blockedHit != null) {
            score -= 20
            reasons += "Contains blocked ingredient: $blockedHit"
        }
        if (template.titleToken in preferences.recentlyHiddenTitles.map { normalizeIngredientName(it) }) {
            score -= 15
            reasons += "Similar to a hidden recipe"
        }
        return TemplateScore(score.coerceIn(0, 100), reasons.distinct())
    }

    private fun expiryPhrase(food: FoodItemEntity, today: LocalDate): String =
        when (ChronoUnit.DAYS.between(today, food.expirationDate)) {
            0L -> "expiring today"
            1L -> "expiring tomorrow"
            else -> "coming up"
        }

    private data class TemplateScore(val value: Int, val reasons: List<String>)

    private data class RecipeTemplate(
        val id: String,
        val baseTitle: String,
        val slots: List<TemplateSlot>,
        val tools: Set<CookingTool>,
        val filters: Set<RecipeFilter>,
        val minutes: Int,
        val titleBuilder: (List<FoodItemEntity>, Map<FoodItemEntity, ClassifiedIngredient>) -> String,
        val stepBuilder: (List<FoodItemEntity>, List<String>) -> List<String>
    ) {
        val requiredCount: Int = slots.count { it.required }
        val titleToken: String = normalizeIngredientName(baseTitle)
        fun title(used: List<FoodItemEntity>, classified: Map<FoodItemEntity, ClassifiedIngredient>): String =
            titleBuilder(used, classified)
        fun steps(used: List<FoodItemEntity>, missing: List<String>): List<String> = stepBuilder(used, missing)
        fun reason(used: List<FoodItemEntity>, expiringSoon: List<FoodItemEntity>, missing: List<String>): String =
            when {
                expiringSoon.isNotEmpty() && missing.isEmpty() -> "Uses ${expiringSoon.first().name.cleanShoppingName()} before its date with what you already have."
                expiringSoon.isNotEmpty() -> "Uses ${expiringSoon.first().name.cleanShoppingName()} soon and only needs ${missing.size} missing item${if (missing.size == 1) "" else "s"}."
                missing.isEmpty() -> "A realistic ${baseTitle.lowercase()} from ingredients you already have."
                else -> "A realistic ${baseTitle.lowercase()} with ${missing.size} item${if (missing.size == 1) "" else "s"} to add."
            }

        fun searchableText(used: List<FoodItemEntity>, missing: List<String>): String =
            normalizeIngredientName(
                (listOf(baseTitle) + slots.flatMap { listOf(it.shoppingLabel) + it.keywords } + used.map { it.name } + missing).joinToString(" ")
            )

        fun confidenceFit(confidence: CookingConfidence): PreferenceScore =
            when (confidence) {
                CookingConfidence.BEGINNER -> if (minutes <= 20) PreferenceScore(10, listOf("Beginner friendly")) else PreferenceScore(-8, emptyList())
                CookingConfidence.COMFORTABLE -> PreferenceScore(4, emptyList())
                CookingConfidence.ADVANCED -> if (minutes >= 25) PreferenceScore(6, listOf("More involved")) else PreferenceScore(0, emptyList())
            }
    }

    private data class TemplateSlot(
        val shoppingLabel: String,
        val categories: Set<IngredientCategory>,
        val keywords: Set<String> = emptySet(),
        val required: Boolean
    ) {
        fun matches(food: FoodItemEntity, classified: ClassifiedIngredient): Boolean {
            if (categories.any { it.compatibleWith(classified.category) }) return true
            val normalized = classified.normalizedName
            return keywords.any { normalized.containsWholeFoodTerm(it) }
        }

        fun isPantryStaple(): Boolean {
            val text = normalizeIngredientName((setOf(shoppingLabel) + keywords).joinToString(" "))
            return pantryStaples.any { text.containsWholeFoodTerm(it) }
        }
    }

    private fun IngredientCategory.compatibleWith(actual: IngredientCategory): Boolean =
        this == actual ||
            this == IngredientCategory.PROTEIN && actual == IngredientCategory.LEFTOVER ||
            this == IngredientCategory.LEFTOVER && actual == IngredientCategory.PROTEIN ||
            this == IngredientCategory.SAUCE && actual == IngredientCategory.CONDIMENT ||
            this == IngredientCategory.CONDIMENT && actual == IngredientCategory.SAUCE

    private fun slot(label: String, required: Boolean, vararg categories: IngredientCategory, keywords: Set<String> = emptySet()): TemplateSlot =
        TemplateSlot(label, categories.toSet(), keywords, required)

    private fun foodName(
        foods: List<FoodItemEntity>,
        classified: Map<FoodItemEntity, ClassifiedIngredient>,
        category: IngredientCategory
    ): String? = foods.firstOrNull { classified.getValue(it).category.compatibleWith(category) }
        ?.let { displayFoodName(it, classified.getValue(it)) }

    private fun firstFoodName(foods: List<FoodItemEntity>, classified: Map<FoodItemEntity, ClassifiedIngredient>, vararg categories: IngredientCategory): String? =
        categories.firstNotNullOfOrNull { category -> foodName(foods, classified, category) }

    private fun displayFoodName(food: FoodItemEntity, classified: ClassifiedIngredient): String {
        val base = classified.subcategory
            ?.takeUnless { it == "leftover" || it == "vegetable" || it == "fruit" || it == "grain" || it == "protein" }
            ?: food.name.cleanShoppingName()
        return base.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun simpleSteps(vararg steps: String): (List<FoodItemEntity>, List<String>) -> List<String> = { _, missing ->
        (if (missing.isNotEmpty()) listOf("Add ${missing.joinToString(" and ")} before cooking.") else emptyList()) + steps.toList()
    }

    private val urgentStatuses = setOf(FreshnessStatus.EXPIRED, FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON)
    private val pantryStaples = setOf("salt", "pepper", "oil", "butter", "sugar", "flour", "garlic powder", "onion powder", "spice", "seasoning", "paprika", "cumin", "oregano", "basil")

    private val templates = listOf(
        RecipeTemplate(
            id = "stir_fry",
            baseTitle = "Stir Fry",
            slots = listOf(
                slot("protein or tofu", true, IngredientCategory.PROTEIN, keywords = setOf("tofu")),
                slot("vegetable", true, IngredientCategory.VEGETABLE),
                slot("rice or noodles", false, IngredientCategory.GRAIN, keywords = setOf("rice", "noodle")),
                slot("sauce", false, IngredientCategory.SAUCE, IngredientCategory.CONDIMENT)
            ),
            tools = setOf(CookingTool.STOVE),
            filters = setOf(RecipeFilter.DINNER, RecipeFilter.LUNCH, RecipeFilter.ONE_PAN),
            minutes = 20,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.PROTEIN), firstFoodName(foods, classified, IngredientCategory.VEGETABLE), "Stir Fry").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Cook protein in a pan.", "Add vegetables and cook until tender.", "Stir in sauce and serve with rice or noodles if available.")
        ),
        RecipeTemplate(
            id = "wrap",
            baseTitle = "Wrap",
            slots = listOf(
                slot("tortilla or wrap", true, IngredientCategory.GRAIN, keywords = setOf("tortilla", "wrap")),
                slot("protein or beans", true, IngredientCategory.PROTEIN, IngredientCategory.CANNED, keywords = setOf("bean")),
                slot("cheese", false, IngredientCategory.DAIRY, keywords = setOf("cheese")),
                slot("lettuce or vegetables", false, IngredientCategory.VEGETABLE),
                slot("sauce", false, IngredientCategory.SAUCE, IngredientCategory.CONDIMENT)
            ),
            tools = setOf(CookingTool.NO_TOOLS, CookingTool.MICROWAVE),
            filters = setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK),
            minutes = 10,
            titleBuilder = { foods, classified ->
                val protein = firstFoodName(foods, classified, IngredientCategory.LEFTOVER, IngredientCategory.PROTEIN) ?: "Bean"
                val hasCheese = firstFoodName(foods, classified, IngredientCategory.DAIRY) != null
                if (foods.any { IngredientClassifier.classify(it).isLeftover } && hasCheese) "Leftover $protein Quesadilla" else "$protein Wrap"
            },
            stepBuilder = simpleSteps("Layer protein or beans on a tortilla.", "Add cheese, vegetables, and sauce if available.", "Roll it cold or microwave briefly until warm.")
        ),
        RecipeTemplate(
            id = "pasta_bowl",
            baseTitle = "Pasta Bowl",
            slots = listOf(
                slot("pasta", true, IngredientCategory.GRAIN, keywords = setOf("pasta", "noodle")),
                slot("protein", false, IngredientCategory.PROTEIN),
                slot("vegetable", false, IngredientCategory.VEGETABLE),
                slot("sauce", false, IngredientCategory.SAUCE, IngredientCategory.CONDIMENT),
                slot("cheese", false, IngredientCategory.DAIRY, keywords = setOf("cheese"))
            ),
            tools = setOf(CookingTool.STOVE),
            filters = setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT),
            minutes = 25,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.PROTEIN, IngredientCategory.VEGETABLE), "Pasta Bowl").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Cook pasta until tender.", "Warm protein, vegetables, and sauce if available.", "Toss everything together and top with cheese if available.")
        ),
        RecipeTemplate(
            id = "scramble",
            baseTitle = "Omelet/Scramble",
            slots = listOf(
                slot("eggs", true, IngredientCategory.PROTEIN, keywords = setOf("egg")),
                slot("cheese", false, IngredientCategory.DAIRY, keywords = setOf("cheese")),
                slot("vegetables", false, IngredientCategory.VEGETABLE),
                slot("cooked meat", false, IngredientCategory.LEFTOVER, IngredientCategory.PROTEIN)
            ),
            tools = setOf(CookingTool.STOVE),
            filters = setOf(RecipeFilter.BREAKFAST, RecipeFilter.DINNER, RecipeFilter.QUICK, RecipeFilter.ONE_PAN),
            minutes = 10,
            titleBuilder = { foods, classified ->
                val veg = firstFoodName(foods, classified, IngredientCategory.VEGETABLE)
                val dairy = firstFoodName(foods, classified, IngredientCategory.DAIRY)
                listOf(veg, dairy, "Egg Scramble").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Whisk eggs.", "Cook vegetables or cooked meat first if using.", "Add eggs and stir gently until set, then add cheese if available.")
        ),
        RecipeTemplate(
            id = "rice_bowl",
            baseTitle = "Rice Bowl",
            slots = listOf(
                slot("rice", true, IngredientCategory.GRAIN, keywords = setOf("rice")),
                slot("protein", false, IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER),
                slot("vegetables", false, IngredientCategory.VEGETABLE),
                slot("sauce", false, IngredientCategory.SAUCE, IngredientCategory.CONDIMENT)
            ),
            tools = setOf(CookingTool.MICROWAVE, CookingTool.STOVE),
            filters = setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK),
            minutes = 15,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER), firstFoodName(foods, classified, IngredientCategory.VEGETABLE), "Rice Bowl").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Warm rice.", "Add protein and vegetables.", "Finish with sauce or dressing if available.")
        ),
        RecipeTemplate(
            id = "soup",
            baseTitle = "Soup",
            slots = listOf(
                slot("broth or canned soup base", true, IngredientCategory.SAUCE, IngredientCategory.CANNED, keywords = setOf("broth", "stock", "soup")),
                slot("vegetables", false, IngredientCategory.VEGETABLE),
                slot("protein", false, IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER),
                slot("noodles or rice", false, IngredientCategory.GRAIN, keywords = setOf("noodle", "rice"))
            ),
            tools = setOf(CookingTool.STOVE),
            filters = setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.COMFORT),
            minutes = 30,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.VEGETABLE, IngredientCategory.PROTEIN), "Soup").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Simmer broth or soup base.", "Add vegetables, protein, and noodles or rice if available.", "Cook until hot and tender.")
        ),
        RecipeTemplate(
            id = "salad",
            baseTitle = "Salad",
            slots = listOf(
                slot("greens or vegetable base", true, IngredientCategory.VEGETABLE, keywords = setOf("lettuce", "greens", "salad")),
                slot("protein", false, IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER),
                slot("cheese", false, IngredientCategory.DAIRY, keywords = setOf("cheese")),
                slot("dressing", false, IngredientCategory.CONDIMENT, IngredientCategory.SAUCE, keywords = setOf("dressing"))
            ),
            tools = setOf(CookingTool.NO_TOOLS),
            filters = setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.NO_COOK, RecipeFilter.HEALTHY),
            minutes = 10,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER, IngredientCategory.VEGETABLE), "Salad").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Add greens or vegetables to a bowl.", "Top with protein and cheese if available.", "Finish with dressing or sauce.")
        ),
        RecipeTemplate(
            id = "smoothie",
            baseTitle = "Smoothie",
            slots = listOf(
                slot("fruit", true, IngredientCategory.FRUIT),
                slot("yogurt or milk", false, IngredientCategory.DAIRY, keywords = setOf("yogurt", "milk")),
                slot("ice", false, IngredientCategory.FROZEN, keywords = setOf("ice", "frozen"))
            ),
            tools = setOf(CookingTool.BLENDER),
            filters = setOf(RecipeFilter.BREAKFAST, RecipeFilter.SNACK, RecipeFilter.QUICK, RecipeFilter.HEALTHY),
            minutes = 5,
            titleBuilder = { foods, classified ->
                listOf(firstFoodName(foods, classified, IngredientCategory.FRUIT), "Smoothie").filterNotNull().joinToString(" ")
            },
            stepBuilder = simpleSteps("Add fruit to a blender.", "Add yogurt, milk, or ice if available.", "Blend until smooth.")
        )
    )
}
