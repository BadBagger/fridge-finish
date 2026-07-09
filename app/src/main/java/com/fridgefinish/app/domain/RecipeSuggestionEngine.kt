package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RecipeFilter(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    QUICK("Quick meal"),
    LEFTOVER_RESCUE("Leftover rescue"),
    NO_COOK("No-cook"),
    ONE_PAN("One-pan"),
    HEALTHY("Healthy"),
    COMFORT("Comfort food")
}

enum class CookingTool(val label: String) {
    MICROWAVE("Microwave"),
    OVEN("Oven"),
    STOVE("Stove"),
    AIR_FRYER("Air fryer"),
    SLOW_COOKER("Slow cooker"),
    NO_TOOLS("No tools"),
    BLENDER("Blender")
}

enum class DietaryStyle(val label: String) {
    NO_PREFERENCE("No preference"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    GLUTEN_FREE("Gluten-free"),
    DAIRY_FREE("Dairy-free"),
    LOW_CARB("Low carb"),
    HIGH_PROTEIN("High protein"),
    BUDGET("Budget meals")
}

enum class RecipeAllergen(val label: String) {
    PEANUTS("Peanuts"),
    TREE_NUTS("Tree nuts"),
    DAIRY("Dairy"),
    EGGS("Eggs"),
    SOY("Soy"),
    WHEAT_GLUTEN("Wheat/gluten"),
    SHELLFISH("Shellfish"),
    FISH("Fish"),
    SESAME("Sesame")
}

enum class SpiceLevel(val label: String) {
    MILD("Mild"),
    MEDIUM("Medium"),
    SPICY("Spicy")
}

enum class CookingConfidence(val label: String) {
    BEGINNER("Beginner"),
    COMFORTABLE("Comfortable"),
    ADVANCED("Advanced")
}

enum class RecipeGenerationSource(val label: String) {
    LOCAL_TEMPLATE("LocalTemplate"),
    AI("AI")
}

data class RecipeSuggestionPreferences(
    val filters: Set<RecipeFilter> = emptySet(),
    val maxMinutes: Int? = null,
    val tools: Set<CookingTool> = emptySet(),
    val blockedIngredients: Set<String> = emptySet(),
    val favoriteIngredients: Set<String> = emptySet(),
    val dietaryRestrictions: Set<String> = emptySet(),
    val dietaryStyle: DietaryStyle = DietaryStyle.NO_PREFERENCE,
    val allergensToAvoid: Set<RecipeAllergen> = emptySet(),
    val spiceLevel: SpiceLevel = SpiceLevel.MILD,
    val cookingConfidence: CookingConfidence = CookingConfidence.COMFORTABLE,
    val recentlyHiddenTitles: Set<String> = emptySet(),
    val assumePantryStaples: Boolean = true,
    val includeExpiredItems: Boolean = false
)

data class RecipeSuggestion(
    val recipe: RecipeEntity,
    val usedFoods: List<FoodItemEntity>,
    val requiredMatchedFoods: List<FoodItemEntity>,
    val optionalMatchedFoods: List<FoodItemEntity>,
    val expiringSoonFoods: List<FoodItemEntity>,
    val missingRequired: List<RecipeIngredientEntity>,
    val matchScore: Int,
    val scoreReasons: List<String>,
    val whySuggested: String,
    val difficulty: String,
    val filters: Set<RecipeFilter>,
    val tools: Set<CookingTool>,
    val safetyNote: String,
    val id: String = "local-recipe-${recipe.id}",
    val title: String = recipe.title,
    val summary: String = recipe.description,
    val ingredientsOwned: List<String> = requiredMatchedFoods.map { it.name.cleanShoppingName() }.distinct(),
    val ingredientsExpiringSoon: List<String> = expiringSoonFoods.map { it.name.cleanShoppingName() }.distinct(),
    val ingredientsMissing: List<String> = missingRequired.map { it.label.cleanShoppingName() }.distinct(),
    val ingredientsOptional: List<String> = optionalMatchedFoods.map { it.name.cleanShoppingName() }.distinct(),
    val steps: List<String> = recipe.steps.split(".").map { it.trim() }.filter { it.isNotBlank() },
    val estimatedTimeMinutes: Int = recipe.minutes,
    val tags: Set<String> = filters.map { it.label }.toSet() + tools.map { it.label },
    val safetyWarnings: List<String> = listOf(safetyNote).filter { it.isNotBlank() },
    val generatedBy: RecipeGenerationSource = RecipeGenerationSource.LOCAL_TEMPLATE,
    val explanation: String = whySuggested
)

object RecipeSuggestionEngine {
    fun buildSuggestions(
        foods: List<FoodItemEntity>,
        recipes: List<RecipeEntity>,
        ingredients: List<RecipeIngredientEntity>,
        preferences: RecipeSuggestionPreferences = RecipeSuggestionPreferences(),
        today: LocalDate = LocalDate.now()
    ): List<RecipeSuggestion> {
        val activeFoods = foods
            .filter { it.isRecipeEligible(preferences, today) }
            .sortedBy { FreshnessCalculator.urgencyRank(it.expirationDate, it.reminderDaysBefore, it.isFinished, today) }
        val ingredientsByRecipe = ingredients.groupBy { it.recipeId }

        return recipes.mapNotNull { recipe ->
            val traits = recipe.inferFilters()
            val tools = recipe.inferTools()
            if (preferences.filters.isNotEmpty() && !traits.containsAll(preferences.filters)) return@mapNotNull null
            if (preferences.maxMinutes != null && recipe.minutes > preferences.maxMinutes) return@mapNotNull null
            if (preferences.tools.isNotEmpty() && preferences.tools.intersect(tools).isEmpty()) return@mapNotNull null

            val recipeIngredients = ingredientsByRecipe[recipe.id].orEmpty()
            if (recipeIngredients.isEmpty()) return@mapNotNull null

            val matched = recipeIngredients.mapNotNull { ingredient ->
                val food = activeFoods.firstOrNull { ingredient.matches(it) }
                if (food == null) null else ingredient to food
            }
            val usedFoods = matched.map { it.second }.distinctBy { it.id }
            if (usedFoods.isEmpty()) return@mapNotNull null
            if (allergenViolations(recipeSearchText(recipe, recipeIngredients, usedFoods), preferences.allergensToAvoid).isNotEmpty()) return@mapNotNull null

            val missingRequired = recipeIngredients
                .filter { ingredient -> ingredient.required && matched.none { match -> match.first == ingredient } }
                .filterNot { ingredient -> preferences.assumePantryStaples && ingredient.isPantryStaple() }
                .distinctBy { it.shoppingKey() }
            if (missingRequired.size > 3) return@mapNotNull null

            val expiringSoon = usedFoods.filter { food ->
                FreshnessCalculator.status(food.expirationDate, food.reminderDaysBefore, food.isFinished, today) in urgentStatuses
            }
            val classifiedFoods = usedFoods.map { IngredientClassifier.classify(it, today) }
            val score = scoreRecipe(
                recipe = recipe,
                recipeIngredients = recipeIngredients,
                usedFoods = usedFoods,
                classifiedFoods = classifiedFoods,
                missingRequired = missingRequired,
                expiringSoon = expiringSoon,
                traits = traits,
                tools = tools,
                preferences = preferences,
                today = today
            )
            RecipeSuggestion(
                recipe = recipe,
                usedFoods = usedFoods,
                requiredMatchedFoods = matched.filter { it.first.required }.map { it.second }.distinctBy { it.id },
                optionalMatchedFoods = matched.filterNot { it.first.required }.map { it.second }.distinctBy { it.id },
                expiringSoonFoods = expiringSoon,
                missingRequired = missingRequired,
                matchScore = score.value,
                scoreReasons = score.reasons,
                whySuggested = why(recipe, usedFoods, classifiedFoods, missingRequired, expiringSoon, today),
                difficulty = recipe.difficulty(),
                filters = traits,
                tools = tools,
                safetyNote = safetyNote(usedFoods, today)
            )
        }.sortedWith(
            compareByDescending<RecipeSuggestion> { it.matchScore }
                .thenBy { it.missingRequired.size }
                .thenByDescending { it.expiringSoonFoods.size }
                .thenBy { it.recipe.minutes }
        )
    }

    private fun scoreRecipe(
        recipe: RecipeEntity,
        recipeIngredients: List<RecipeIngredientEntity>,
        usedFoods: List<FoodItemEntity>,
        classifiedFoods: List<ClassifiedIngredient>,
        missingRequired: List<RecipeIngredientEntity>,
        expiringSoon: List<FoodItemEntity>,
        traits: Set<RecipeFilter>,
        tools: Set<CookingTool>,
        preferences: RecipeSuggestionPreferences,
        today: LocalDate
    ): RecipeScore {
        val reasons = mutableListOf<String>()
        var score = 25

        val todayOrTomorrow = usedFoods.filter { food ->
            val days = ChronoUnit.DAYS.between(today, food.expirationDate)
            days in 0..1
        }
        if (todayOrTomorrow.isNotEmpty()) {
            score += 25
            reasons += "Saves ${todayOrTomorrow.first().name.cleanShoppingName()} ${expiryPhrase(todayOrTomorrow.first(), today)}"
        }
        if (expiringSoon.size >= 2) {
            score += 15
            reasons += "Uses ${expiringSoon.size} ingredients that need attention"
        }

        val ingredientCountForRatio = recipeIngredients
            .count { !(preferences.assumePantryStaples && it.isPantryStaple()) }
            .coerceAtLeast(1)
        val ownedRatio = usedFoods.size.toDouble() / ingredientCountForRatio
        if (ownedRatio >= 0.7 || missingRequired.isEmpty()) {
            score += 20
            reasons += "Uses ${usedFoods.size} ingredient${if (usedFoods.size == 1) "" else "s"} you already have"
        } else if (usedFoods.isNotEmpty()) {
            score += (ownedRatio * 14).toInt()
            reasons += "Uses ${usedFoods.size} ingredient${if (usedFoods.size == 1) "" else "s"} you already have"
        }

        if (classifiedFoods.any { it.isLeftover || it.category == IngredientCategory.LEFTOVER }) {
            score += 10
            reasons += "Uses leftovers"
        }

        val selectedMealTypes = preferences.filters.intersect(mealTypeFilters)
        if (selectedMealTypes.isNotEmpty() && traits.any { it in selectedMealTypes }) {
            score += 10
            reasons += "Matches selected meal type"
        }

        if (preferences.maxMinutes != null && recipe.minutes <= preferences.maxMinutes) {
            score += 10
            reasons += "Fits your selected cooking time"
        }

        val selectedPreferenceFilters = preferences.filters - mealTypeFilters
        if (selectedPreferenceFilters.isNotEmpty() && selectedPreferenceFilters.all { it in traits }) {
            score += 8
            reasons += "Matches your recipe preferences"
        }
        if (preferences.tools.isNotEmpty() && preferences.tools.intersect(tools).isNotEmpty()) {
            score += 6
            reasons += "Works with selected cooking tools"
        }

        val searchableText = recipeSearchText(recipe, recipeIngredients, usedFoods)
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
        val dietFit = dietaryStyleFit(searchableText, preferences.dietaryStyle, classifiedFoods)
        score += dietFit.score
        reasons += dietFit.reasons
        val spiceFit = spiceLevelFit(searchableText, preferences.spiceLevel)
        score += spiceFit.score
        reasons += spiceFit.reasons
        val confidenceFit = confidenceFit(recipe, preferences.cookingConfidence)
        score += confidenceFit.score
        reasons += confidenceFit.reasons

        val wasteReduction = (classifiedFoods.sumOf { it.priorityScore } / 10).coerceAtMost(15)
        if (wasteReduction > 0) {
            score += wasteReduction
            if (wasteReduction >= 8) reasons += "Good food-waste reduction match"
        }

        val synergy = synergyScore(classifiedFoods)
        score += synergy
        if (synergy >= 10) reasons += "Uses a realistic meal pattern"

        if (missingRequired.isNotEmpty()) {
            score -= missingRequired.size * 10
            reasons += if (missingRequired.size == 1) {
                "Only missing ${missingRequired.first().shoppingLabel()}"
            } else {
                "Missing ${missingRequired.size} major ingredients"
            }
        } else if (preferences.assumePantryStaples && recipeIngredients.any { it.isPantryStaple() }) {
            reasons += "Pantry basics assumed"
        }

        val dietViolations = dietaryViolations(recipe, recipeIngredients, usedFoods, preferences.dietaryRestrictions)
        if (dietViolations.isNotEmpty()) {
            score -= 30 * dietViolations.size
            reasons += "Conflicts with ${dietViolations.first()}"
        }

        if (isSimilarToHidden(recipe.title, preferences.recentlyHiddenTitles)) {
            score -= 15
            reasons += "Similar to a hidden recipe"
        }

        return RecipeScore(score.coerceIn(0, 100), reasons.distinct())
    }

    private fun why(
        recipe: RecipeEntity,
        usedFoods: List<FoodItemEntity>,
        classifiedFoods: List<ClassifiedIngredient>,
        missingRequired: List<RecipeIngredientEntity>,
        expiringSoon: List<FoodItemEntity>,
        today: LocalDate
    ): String {
        val urgent = expiringSoon.minByOrNull { ChronoUnit.DAYS.between(today, it.expirationDate) }
        val mealPattern = mealPattern(classifiedFoods)
        return when {
            mealPattern != null && urgent != null -> "Matches a realistic $mealPattern and uses ${urgent.name.cleanShoppingName()} before its date."
            mealPattern != null && missingRequired.isEmpty() -> "Matches a realistic $mealPattern with ingredients you already have."
            urgent != null && missingRequired.isEmpty() -> "Uses ${urgent.name.cleanShoppingName()} before its date, and you already have everything needed."
            urgent != null -> "Uses ${urgent.name.cleanShoppingName()} soon and only needs ${missingRequired.size} missing item${if (missingRequired.size == 1) "" else "s"}."
            missingRequired.isEmpty() -> "You already have the main ingredients, and it takes ${recipe.minutes} minutes."
            else -> "Good match for what you have with ${missingRequired.size} item${if (missingRequired.size == 1) "" else "s"} to add."
        }
    }

    private fun synergyScore(classifiedFoods: List<ClassifiedIngredient>): Int {
        val categories = classifiedFoods.map { it.category }.toSet()
        return when {
            hasMealPattern(categories, IngredientCategory.PROTEIN, IngredientCategory.VEGETABLE, IngredientCategory.GRAIN) -> 14
            hasEggVegetableDairy(classifiedFoods) -> 12
            IngredientCategory.LEFTOVER in categories && IngredientCategory.GRAIN in categories && IngredientCategory.DAIRY in categories -> 12
            IngredientCategory.FRUIT in categories && IngredientCategory.DAIRY in categories -> 10
            IngredientCategory.VEGETABLE in categories && (IngredientCategory.SAUCE in categories || IngredientCategory.GRAIN in categories) -> 8
            IngredientCategory.GRAIN in categories && IngredientCategory.DAIRY in categories && IngredientCategory.PROTEIN in categories -> 8
            else -> 0
        }
    }

    private fun mealPattern(classifiedFoods: List<ClassifiedIngredient>): String? {
        val categories = classifiedFoods.map { it.category }.toSet()
        return when {
            hasMealPattern(categories, IngredientCategory.PROTEIN, IngredientCategory.VEGETABLE, IngredientCategory.GRAIN) -> "bowl, stir fry, wrap, skillet, soup, or casserole"
            hasEggVegetableDairy(classifiedFoods) -> "omelet, scramble, breakfast wrap, or frittata"
            IngredientCategory.LEFTOVER in categories && IngredientCategory.GRAIN in categories && IngredientCategory.DAIRY in categories -> "quesadilla or wrap"
            IngredientCategory.FRUIT in categories && IngredientCategory.DAIRY in categories -> "smoothie or parfait"
            IngredientCategory.VEGETABLE in categories && IngredientCategory.SAUCE in categories -> "soup or saucy vegetable meal"
            IngredientCategory.GRAIN in categories && IngredientCategory.DAIRY in categories && IngredientCategory.PROTEIN in categories -> "sandwich or melt"
            else -> null
        }
    }

    private fun hasMealPattern(categories: Set<IngredientCategory>, vararg required: IngredientCategory): Boolean =
        required.all { it in categories }

    private fun hasEggVegetableDairy(classifiedFoods: List<ClassifiedIngredient>): Boolean {
        val categories = classifiedFoods.map { it.category }.toSet()
        val hasEgg = classifiedFoods.any { it.subcategory == "egg" || it.normalizedName.containsWholeFoodTerm("egg") }
        return hasEgg && IngredientCategory.VEGETABLE in categories && IngredientCategory.DAIRY in categories
    }

    private fun safetyNote(foods: List<FoodItemEntity>, today: LocalDate): String {
        val hasSpoiled = foods.any { it.itemState == FoodItemState.SPOILED }
        val hasQuestionable = foods.any { it.itemState == FoodItemState.QUESTIONABLE }
        val hasExpired = foods.any { it.itemState == FoodItemState.EXPIRED || it.expirationDate.isBefore(today) }
        val hasAgedLeftovers = foods.any { it.isAgedLeftover(today) }
        val hasHighRisk = foods.any { it.isHighRiskFood() }
        return when {
            hasSpoiled -> "Spoiled items are not recipe ingredients. Dispose of them instead."
            hasQuestionable -> "Use only if still safe. Check smell, texture, and package guidance before using."
            hasExpired -> "Includes a past-date item. Check smell, texture, and package guidance before using."
            hasAgedLeftovers -> "Leftovers can become risky after several days. Check smell, texture, date, and reheat thoroughly."
            hasHighRisk -> "High-risk foods need extra caution. Check smell, texture, and package guidance before using."
            else -> "Dates are reminders, not safety guarantees. Check food before eating."
        }
    }

    private fun RecipeEntity.inferFilters(): Set<RecipeFilter> {
        val text = "$title $description $steps".lowercase()
        return buildSet {
            if (minutes <= 15) add(RecipeFilter.QUICK)
            if (listOf("omelet", "yogurt", "smoothie").any { text.contains(it) }) add(RecipeFilter.BREAKFAST)
            if (listOf("salad", "grain bowl", "quesadilla", "soup", "wrap", "sandwich").any { text.contains(it) }) add(RecipeFilter.LUNCH)
            if (listOf("pasta", "rice", "soup", "quesadilla", "omelet", "wrap", "skillet").any { text.contains(it) }) add(RecipeFilter.DINNER)
            if (listOf("snack", "plate", "yogurt", "smoothie").any { text.contains(it) }) add(RecipeFilter.SNACK)
            if (text.contains("leftover")) add(RecipeFilter.LEFTOVER_RESCUE)
            if (listOf("smoothie", "yogurt", "salad", "snack").any { text.contains(it) }) add(RecipeFilter.NO_COOK)
            if (listOf("omelet", "pasta", "rice", "soup", "quesadilla").any { text.contains(it) }) add(RecipeFilter.ONE_PAN)
            if (listOf("salad", "smoothie", "yogurt", "vegetable", "greens", "fruit").any { text.contains(it) }) add(RecipeFilter.HEALTHY)
            if (listOf("pasta", "quesadilla", "soup", "fried rice", "cheese").any { text.contains(it) }) add(RecipeFilter.COMFORT)
        }
    }

    private fun RecipeEntity.inferTools(): Set<CookingTool> {
        val text = "$title $description $steps".lowercase()
        return buildSet {
            if (listOf("smoothie", "yogurt", "salad", "snack plate").any { text.contains(it) }) add(CookingTool.NO_TOOLS)
            if (listOf("warm", "leftover").any { text.contains(it) }) add(CookingTool.MICROWAVE)
            if (listOf("omelet", "pasta", "soup", "fried rice", "quesadilla", "stir-fry", "cook", "simmer", "toast").any { text.contains(it) }) add(CookingTool.STOVE)
            if (listOf("pizza", "bake", "roast").any { text.contains(it) }) add(CookingTool.OVEN)
            if (listOf("air fryer", "crispy").any { text.contains(it) }) add(CookingTool.AIR_FRYER)
            if (listOf("slow cooker", "slow-cooker").any { text.contains(it) }) add(CookingTool.SLOW_COOKER)
        }.ifEmpty { setOf(CookingTool.STOVE) }
    }

    private fun RecipeEntity.difficulty(): String = when {
        minutes <= 10 -> "Easy"
        minutes <= 30 -> "Moderate"
        else -> "More involved"
    }

    private fun RecipeIngredientEntity.matches(food: FoodItemEntity): Boolean =
        recipeIngredientMatchesFood(label, keywords, category, food.name, food.category)

    private fun RecipeIngredientEntity.shoppingKey(): String = label.trim().lowercase()

    private fun RecipeIngredientEntity.isPantryStaple(): Boolean {
        val text = normalizeIngredientName("$label $keywords")
        return pantryStaples.any { text.containsWholeFoodTerm(it) }
    }

    private fun RecipeIngredientEntity.shoppingLabel(): String {
        val clean = label.trim()
        return when (clean.lowercase()) {
            "produce", "vegetables", "vegetable" -> "vegetables"
            "fruit" -> "fruit"
            "protein" -> "protein"
            "grain", "rice or grain" -> "rice or grain"
            "dairy" -> "dairy"
            else -> clean.ifBlank { "ingredient" }
        }
    }

    private fun expiryPhrase(food: FoodItemEntity, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, food.expirationDate)
        return when (days) {
            0L -> "expiring today"
            1L -> "expiring tomorrow"
            else -> "coming up"
        }
    }

    private fun blockedIngredientHits(
        recipe: RecipeEntity,
        recipeIngredients: List<RecipeIngredientEntity>,
        usedFoods: List<FoodItemEntity>,
        blockedIngredients: Set<String>
    ): List<String> {
        val blocked = blockedIngredients.map { normalizeIngredientName(it) }.filter { it.isNotBlank() }
        if (blocked.isEmpty()) return emptyList()
        val searchable = listOf(recipe.title, recipe.description, recipe.steps) +
            recipeIngredients.flatMap { listOf(it.label, it.keywords) } +
            usedFoods.map { it.name }
        return blocked.filter { blockedTerm -> searchable.any { normalizeIngredientName(it).containsWholeFoodTerm(blockedTerm) } }
    }

    private fun dietaryViolations(
        recipe: RecipeEntity,
        recipeIngredients: List<RecipeIngredientEntity>,
        usedFoods: List<FoodItemEntity>,
        dietaryRestrictions: Set<String>
    ): List<String> {
        val restrictions = dietaryRestrictions.map { normalizeIngredientName(it) }.toSet()
        if (restrictions.isEmpty()) return emptyList()
        val text = (listOf(recipe.title, recipe.description, recipe.steps) + recipeIngredients.flatMap { listOf(it.label, it.keywords) } + usedFoods.map { it.name })
            .joinToString(" ") { normalizeIngredientName(it) }
        return buildList {
            if ("vegetarian" in restrictions && meatTerms.any { text.containsWholeFoodTerm(it) }) add("vegetarian preference")
            if ("dairy free" in restrictions && dairyTerms.any { text.containsWholeFoodTerm(it) }) add("dairy-free preference")
            if ("gluten free" in restrictions && glutenTerms.any { text.containsWholeFoodTerm(it) }) add("gluten-free preference")
        }
    }

    private fun recipeSearchText(
        recipe: RecipeEntity,
        recipeIngredients: List<RecipeIngredientEntity>,
        usedFoods: List<FoodItemEntity>
    ): String =
        (listOf(recipe.title, recipe.description, recipe.steps) +
            recipeIngredients.flatMap { listOf(it.label, it.keywords, it.category.orEmpty()) } +
            usedFoods.map { it.name })
            .joinToString(" ") { normalizeIngredientName(it) }

    private fun isSimilarToHidden(title: String, hiddenTitles: Set<String>): Boolean {
        val titleTokens = normalizeIngredientName(title).split(" ").filter { it.length >= 4 }.toSet()
        if (titleTokens.isEmpty()) return false
        return hiddenTitles.any { hidden ->
            val hiddenTokens = normalizeIngredientName(hidden).split(" ").filter { it.length >= 4 }.toSet()
            titleTokens.intersect(hiddenTokens).isNotEmpty()
        }
    }

    private data class RecipeScore(val value: Int, val reasons: List<String>)

    private val urgentStatuses = setOf(
        FreshnessStatus.EXPIRED,
        FreshnessStatus.EXPIRES_TODAY,
        FreshnessStatus.EAT_SOON
    )

    private val mealTypeFilters = setOf(RecipeFilter.BREAKFAST, RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.SNACK)
    private val pantryStaples = setOf("salt", "pepper", "oil", "butter", "sugar", "flour", "garlic powder", "onion powder", "spice", "seasoning", "paprika", "cumin", "oregano", "basil")
    private val meatTerms = setOf("chicken", "beef", "pork", "turkey", "fish", "salmon", "shrimp", "ham", "bacon", "sausage", "meat")
    private val dairyTerms = setOf("milk", "cheese", "cheddar", "yogurt", "cream", "butter")
    private val glutenTerms = setOf("bread", "pasta", "noodle", "tortilla", "wrap", "flour", "cracker")
}

data class PreferenceScore(val score: Int, val reasons: List<String>)

fun allergenViolations(searchableText: String, allergens: Set<RecipeAllergen>): List<RecipeAllergen> =
    allergens.filter { allergen ->
        allergenTerms.getValue(allergen).any { searchableText.containsWholeFoodTerm(it) }
    }

fun termHits(searchableText: String, rawTerms: Set<String>): List<String> =
    rawTerms.map { normalizeIngredientName(it) }
        .filter { it.isNotBlank() }
        .filter { searchableText.containsWholeFoodTerm(it) }
        .distinct()

fun dietaryStyleFit(
    searchableText: String,
    style: DietaryStyle,
    classifiedFoods: List<ClassifiedIngredient>
): PreferenceScore =
    when (style) {
        DietaryStyle.NO_PREFERENCE -> PreferenceScore(0, emptyList())
        DietaryStyle.VEGETARIAN -> if (meatPreferenceTerms.any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(-30, listOf("Conflicts with vegetarian"))
        } else {
            PreferenceScore(10, listOf("Vegetarian"))
        }
        DietaryStyle.VEGAN -> if ((meatPreferenceTerms + dairyPreferenceTerms + eggPreferenceTerms).any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(-30, listOf("Conflicts with vegan"))
        } else {
            PreferenceScore(10, listOf("Vegan"))
        }
        DietaryStyle.GLUTEN_FREE -> if (glutenPreferenceTerms.any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(-30, listOf("Conflicts with gluten-free"))
        } else {
            PreferenceScore(10, listOf("Gluten-free"))
        }
        DietaryStyle.DAIRY_FREE -> if (dairyPreferenceTerms.any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(-30, listOf("Conflicts with dairy-free"))
        } else {
            PreferenceScore(10, listOf("No dairy"))
        }
        DietaryStyle.LOW_CARB -> if (lowCarbPenaltyTerms.any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(-8, listOf("Higher-carb ingredients"))
        } else {
            PreferenceScore(8, listOf("Lower carb"))
        }
        DietaryStyle.HIGH_PROTEIN -> if (classifiedFoods.any { it.category in setOf(IngredientCategory.PROTEIN, IngredientCategory.LEFTOVER) }) {
            PreferenceScore(12, listOf("High protein"))
        } else {
            PreferenceScore(-6, emptyList())
        }
        DietaryStyle.BUDGET -> if (budgetFriendlyTerms.any { searchableText.containsWholeFoodTerm(it) }) {
            PreferenceScore(8, listOf("Budget friendly"))
        } else {
            PreferenceScore(0, emptyList())
        }
    }

fun spiceLevelFit(searchableText: String, spiceLevel: SpiceLevel): PreferenceScore {
    val hasSpicy = spicyTerms.any { searchableText.containsWholeFoodTerm(it) }
    return when {
        spiceLevel == SpiceLevel.MILD && hasSpicy -> PreferenceScore(-10, listOf("Spicier than mild preference"))
        spiceLevel == SpiceLevel.SPICY && hasSpicy -> PreferenceScore(8, listOf("Spicy"))
        spiceLevel == SpiceLevel.MEDIUM -> PreferenceScore(2, emptyList())
        else -> PreferenceScore(0, emptyList())
    }
}

fun confidenceFit(recipe: RecipeEntity, confidence: CookingConfidence): PreferenceScore =
    when (confidence) {
        CookingConfidence.BEGINNER -> if (recipe.minutes <= 20) {
            PreferenceScore(10, listOf("Beginner friendly"))
        } else {
            PreferenceScore(-8, emptyList())
        }
        CookingConfidence.COMFORTABLE -> PreferenceScore(4, emptyList())
        CookingConfidence.ADVANCED -> if (recipe.minutes >= 25) {
            PreferenceScore(6, listOf("More involved"))
        } else {
            PreferenceScore(0, emptyList())
        }
    }

val allergenTerms: Map<RecipeAllergen, Set<String>> = mapOf(
    RecipeAllergen.PEANUTS to setOf("peanut", "peanuts", "peanut butter"),
    RecipeAllergen.TREE_NUTS to setOf("almond", "walnut", "pecan", "cashew", "pistachio", "hazelnut", "nut", "nuts"),
    RecipeAllergen.DAIRY to setOf("milk", "cheese", "cheddar", "yogurt", "cream", "butter", "dairy"),
    RecipeAllergen.EGGS to setOf("egg", "eggs"),
    RecipeAllergen.SOY to setOf("soy", "soy sauce", "tofu", "tempeh"),
    RecipeAllergen.WHEAT_GLUTEN to setOf("wheat", "gluten", "bread", "pasta", "noodle", "tortilla", "wrap", "flour", "cracker"),
    RecipeAllergen.SHELLFISH to setOf("shrimp", "crab", "lobster", "shellfish"),
    RecipeAllergen.FISH to setOf("fish", "salmon", "tuna", "cod"),
    RecipeAllergen.SESAME to setOf("sesame", "tahini")
)

private val meatPreferenceTerms = setOf("chicken", "beef", "pork", "turkey", "fish", "salmon", "shrimp", "ham", "bacon", "sausage", "meat", "seafood")
private val dairyPreferenceTerms = setOf("milk", "cheese", "cheddar", "yogurt", "cream", "butter", "dairy")
private val eggPreferenceTerms = setOf("egg", "eggs")
private val glutenPreferenceTerms = setOf("wheat", "gluten", "bread", "pasta", "noodle", "tortilla", "wrap", "flour", "cracker")
private val lowCarbPenaltyTerms = setOf("rice", "pasta", "bread", "tortilla", "wrap", "noodle", "potato", "flour", "sugar")
private val budgetFriendlyTerms = setOf("rice", "bean", "pasta", "egg", "leftover", "potato", "canned")
private val spicyTerms = setOf("spicy", "hot sauce", "jalapeno", "chili", "sriracha", "cayenne")

fun FoodItemEntity.isRecipeEligible(
    preferences: RecipeSuggestionPreferences,
    today: LocalDate = LocalDate.now()
): Boolean {
    if (isFinished || itemState == FoodItemState.SPOILED) return false
    val expired = itemState == FoodItemState.EXPIRED || expirationDate.isBefore(today)
    if (expired && !preferences.includeExpiredItems) return false
    return true
}

fun FoodItemEntity.isHighRiskFood(): Boolean {
    val text = normalizeIngredientName(name)
    return category in setOf(FoodCategory.MEAT, FoodCategory.DAIRY, FoodCategory.LEFTOVERS) ||
        ingredientCategory in setOf(IngredientCategory.PROTEIN, IngredientCategory.DAIRY, IngredientCategory.LEFTOVER) ||
        text.containsWholeFoodTerm("raw chicken") ||
        text.containsWholeFoodTerm("seafood") ||
        text.containsWholeFoodTerm("ground meat") ||
        text.containsWholeFoodTerm("deli meat") ||
        text.containsWholeFoodTerm("leftover")
}

fun FoodItemEntity.isAgedLeftover(today: LocalDate = LocalDate.now()): Boolean {
    if (!(isLeftover || category == FoodCategory.LEFTOVERS || ingredientCategory == IngredientCategory.LEFTOVER)) return false
    val start = openedDate ?: purchaseDate ?: createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(start, today) >= 3
}
