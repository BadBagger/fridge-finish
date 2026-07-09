package com.fridgefinish.app.domain

import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecipeSuggestionEngineTest {
    private val today = LocalDate.of(2026, 7, 8)

    @Test
    fun prioritizesExpiringFoodAndOwnedIngredients() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("Lettuce", FoodCategory.PRODUCE, today),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(3)),
                food("Milk", FoodCategory.DAIRY, today.plusDays(10))
            ),
            recipes = listOf(
                recipe(1, "Simple salad", 10, "Use greens", "Combine greens with dressing."),
                recipe(2, "Rice bowl", 20, "Use pantry base", "Warm rice with toppings.")
            ),
            ingredients = listOf(
                ingredient(1, "lettuce or spinach", "lettuce,spinach", "PRODUCE"),
                ingredient(1, "dressing", "dressing,sauce", "CONDIMENTS", required = false),
                ingredient(2, "rice", "rice", "PANTRY")
            ),
            today = today
        )

        assertEquals("Simple salad", suggestions.first().recipe.title)
        assertTrue(suggestions.first().expiringSoonFoods.any { it.name == "Lettuce" })
        assertTrue(suggestions.first().matchScore > suggestions.last().matchScore)
    }

    @Test
    fun prefersFewerMissingIngredients() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Eggs", FoodCategory.DAIRY, today.plusDays(2))),
            recipes = listOf(
                recipe(1, "Quick omelet", 15, "Use eggs", "Whisk eggs and cook."),
                recipe(2, "Full breakfast", 20, "Use eggs with extras", "Cook eggs with sides.")
            ),
            ingredients = listOf(
                ingredient(1, "eggs", "egg", "DAIRY"),
                ingredient(2, "eggs", "egg", "DAIRY"),
                ingredient(2, "toast", "bread,toast", "PANTRY"),
                ingredient(2, "fruit", "fruit,berry,banana", "PRODUCE")
            ),
            today = today
        )

        assertEquals("Quick omelet", suggestions.first().recipe.title)
        assertEquals(0, suggestions.first().missingRequired.size)
    }

    @Test
    fun appliesFilterTimeAndToolPreferences() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Yogurt", FoodCategory.DAIRY, today.plusDays(2))),
            recipes = listOf(
                recipe(1, "Yogurt fruit bowl", 5, "A fast way to use fruit and yogurt.", "Spoon yogurt into a bowl."),
                recipe(2, "Soup starter", 30, "Simmer vegetables.", "Simmer soup on the stove.")
            ),
            ingredients = listOf(
                ingredient(1, "yogurt", "yogurt", "DAIRY"),
                ingredient(2, "broth", "broth", "PANTRY")
            ),
            preferences = RecipeSuggestionPreferences(
                filters = setOf(RecipeFilter.BREAKFAST, RecipeFilter.NO_COOK),
                maxMinutes = 5,
                tools = setOf(CookingTool.NO_TOOLS)
            ),
            today = today
        )

        assertEquals(listOf("Yogurt fruit bowl"), suggestions.map { it.recipe.title })
    }

    @Test
    fun warnsWhenPastDateFoodIsUsed() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken leftovers", FoodCategory.LEFTOVERS, today.minusDays(1))),
            recipes = listOf(recipe(1, "Leftover grain bowl", 12, "Use leftovers first.", "Warm leftovers with rice.")),
            ingredients = listOf(ingredient(1, "leftovers", "leftover,chicken", "LEFTOVERS")),
            preferences = RecipeSuggestionPreferences(includeExpiredItems = true),
            today = today
        )

        assertTrue(suggestions.single().safetyNote.contains("past-date", ignoreCase = true))
    }

    @Test
    fun categoryPatternBoostsRealisticBowlSuggestion() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("chix breast", FoodCategory.MEAT, today.plusDays(1)),
                food("bell peppers", FoodCategory.PRODUCE, today.plusDays(2)),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(4))
            ),
            recipes = listOf(
                recipe(1, "Protein vegetable grain bowl", 18, "Use a full meal pattern.", "Warm grain, protein, and vegetables."),
                recipe(2, "Plain rice", 10, "Use pantry rice.", "Warm rice.")
            ),
            ingredients = listOf(
                ingredient(1, "protein", "protein", "PROTEIN"),
                ingredient(1, "vegetable", "vegetable", "VEGETABLE"),
                ingredient(1, "grain", "grain,rice", "GRAIN"),
                ingredient(2, "rice", "rice", "GRAIN")
            ),
            today = today
        )

        assertEquals("Protein vegetable grain bowl", suggestions.first().recipe.title)
        assertTrue(suggestions.first().whySuggested.contains("bowl", ignoreCase = true))
        assertTrue(suggestions.first().matchScore > suggestions.last().matchScore)
    }

    @Test
    fun expiringFruitAndDairyBecomeSmoothieOrParfait() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("tomatoes", FoodCategory.PRODUCE, today.plusDays(2)),
                food("Bananas", FoodCategory.PRODUCE, today),
                food("Greek yogurt", FoodCategory.DAIRY, today.plusDays(5))
            ),
            recipes = listOf(recipe(1, "Fruit yogurt smoothie or parfait", 5, "Use fruit and dairy.", "Blend or layer.")),
            ingredients = listOf(
                ingredient(1, "fruit", "fruit", "FRUIT"),
                ingredient(1, "yogurt or milk", "yogurt,milk", "DAIRY")
            ),
            today = today
        )

        assertEquals("Fruit yogurt smoothie or parfait", suggestions.single().recipe.title)
        assertTrue(suggestions.single().expiringSoonFoods.any { it.name == "Bananas" })
        assertTrue(suggestions.single().whySuggested.contains("smoothie", ignoreCase = true))
    }

    @Test
    fun scoreExplainsOwnedExpiringAndMissingIngredients() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("Chicken breast", FoodCategory.MEAT, today.plusDays(1)),
                food("Bell pepper", FoodCategory.PRODUCE, today.plusDays(3))
            ),
            recipes = listOf(recipe(1, "Chicken wrap", 15, "Use chicken and vegetables.", "Add chicken and peppers to tortilla.")),
            ingredients = listOf(
                ingredient(1, "protein", "protein", "PROTEIN"),
                ingredient(1, "vegetable", "vegetable", "VEGETABLE"),
                ingredient(1, "tortillas", "tortilla,wrap", "GRAIN")
            ),
            preferences = RecipeSuggestionPreferences(
                filters = setOf(RecipeFilter.LUNCH),
                maxMinutes = 15
            ),
            today = today
        )

        val reasons = suggestions.single().scoreReasons.joinToString(" ")
        assertTrue(reasons.contains("Saves Chicken breast expiring tomorrow"))
        assertTrue(reasons.contains("Uses 2 ingredients you already have"))
        assertTrue(reasons.contains("Only missing tortillas"))
    }

    @Test
    fun blockedIngredientAndDietRestrictionLowerScore() {
        val base = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken breast", FoodCategory.MEAT, today.plusDays(1))),
            recipes = listOf(recipe(1, "Chicken bowl", 15, "Use chicken.", "Cook chicken.")),
            ingredients = listOf(ingredient(1, "protein", "protein", "PROTEIN")),
            today = today
        ).single()

        val blocked = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken breast", FoodCategory.MEAT, today.plusDays(1))),
            recipes = listOf(recipe(1, "Chicken bowl", 15, "Use chicken.", "Cook chicken.")),
            ingredients = listOf(ingredient(1, "protein", "protein", "PROTEIN")),
            preferences = RecipeSuggestionPreferences(
                blockedIngredients = setOf("chicken"),
                dietaryRestrictions = setOf("vegetarian")
            ),
            today = today
        ).single()

        assertTrue(blocked.matchScore < base.matchScore)
        assertTrue(blocked.scoreReasons.any { it.contains("blocked", ignoreCase = true) })
        assertTrue(blocked.scoreReasons.any { it.contains("vegetarian", ignoreCase = true) })
    }

    @Test
    fun recentlyHiddenSimilarRecipeGetsPenalty() {
        val base = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Rice", FoodCategory.PANTRY, today.plusDays(30))),
            recipes = listOf(recipe(1, "Rice bowl", 15, "Use rice.", "Warm rice.")),
            ingredients = listOf(ingredient(1, "rice", "rice", "GRAIN")),
            today = today
        ).single()

        val hiddenSimilar = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Rice", FoodCategory.PANTRY, today.plusDays(30))),
            recipes = listOf(recipe(1, "Rice bowl", 15, "Use rice.", "Warm rice.")),
            ingredients = listOf(ingredient(1, "rice", "rice", "GRAIN")),
            preferences = RecipeSuggestionPreferences(recentlyHiddenTitles = setOf("Chicken rice bowl")),
            today = today
        ).single()

        assertTrue(hiddenSimilar.matchScore < base.matchScore)
        assertTrue(hiddenSimilar.scoreReasons.any { it.contains("hidden", ignoreCase = true) })
    }

    @Test
    fun pantryStaplesAreAssumedByDefaultButCanBeRequired() {
        val defaultSuggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken breast", FoodCategory.MEAT, today.plusDays(1))),
            recipes = listOf(recipe(1, "Simple chicken", 15, "Use chicken with basics.", "Cook chicken with oil, salt, and pepper.")),
            ingredients = listOf(
                ingredient(1, "chicken", "chicken", "PROTEIN"),
                ingredient(1, "oil", "oil", "PANTRY"),
                ingredient(1, "salt and pepper", "salt,pepper", "SPICE")
            ),
            today = today
        ).single()

        val requireStaples = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken breast", FoodCategory.MEAT, today.plusDays(1))),
            recipes = listOf(recipe(1, "Simple chicken", 15, "Use chicken with basics.", "Cook chicken with oil, salt, and pepper.")),
            ingredients = listOf(
                ingredient(1, "chicken", "chicken", "PROTEIN"),
                ingredient(1, "oil", "oil", "PANTRY"),
                ingredient(1, "salt and pepper", "salt,pepper", "SPICE")
            ),
            preferences = RecipeSuggestionPreferences(assumePantryStaples = false),
            today = today
        ).single()

        assertEquals(0, defaultSuggestions.missingRequired.size)
        assertTrue(defaultSuggestions.scoreReasons.any { it.contains("Pantry basics assumed", ignoreCase = true) })
        assertEquals(2, requireStaples.missingRequired.size)
    }

    @Test
    fun spoiledItemsAreExcludedFromRecipeSuggestions() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Chicken breast", FoodCategory.MEAT, today.plusDays(1)).copy(itemState = FoodItemState.SPOILED)),
            recipes = listOf(recipe(1, "Chicken bowl", 15, "Use chicken.", "Cook chicken.")),
            ingredients = listOf(ingredient(1, "chicken", "chicken", "PROTEIN")),
            preferences = RecipeSuggestionPreferences(includeExpiredItems = true),
            today = today
        )

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun expiredItemsRequireOptInAndShowWarning() {
        val base = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Rice", FoodCategory.PANTRY, today.minusDays(1))),
            recipes = listOf(recipe(1, "Rice bowl", 15, "Use rice.", "Warm rice.")),
            ingredients = listOf(ingredient(1, "rice", "rice", "GRAIN")),
            today = today
        )

        val included = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Rice", FoodCategory.PANTRY, today.minusDays(1))),
            recipes = listOf(recipe(1, "Rice bowl", 15, "Use rice.", "Warm rice.")),
            ingredients = listOf(ingredient(1, "rice", "rice", "GRAIN")),
            preferences = RecipeSuggestionPreferences(includeExpiredItems = true),
            today = today
        )

        assertTrue(base.isEmpty())
        assertEquals(1, included.size)
        assertTrue(included.single().safetyNote.contains("package guidance", ignoreCase = true))
    }

    @Test
    fun questionableItemsShowUseOnlyIfSafeWarning() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Milk", FoodCategory.DAIRY, today.plusDays(2)).copy(itemState = FoodItemState.QUESTIONABLE)),
            recipes = listOf(recipe(1, "Milk smoothie", 5, "Use milk.", "Blend milk with fruit.")),
            ingredients = listOf(ingredient(1, "milk", "milk", "DAIRY")),
            today = today
        )

        assertTrue(suggestions.single().safetyNote.contains("Use only if still safe", ignoreCase = true))
    }

    @Test
    fun allergensToAvoidHardBlockRecipes() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Milk", FoodCategory.DAIRY, today.plusDays(2))),
            recipes = listOf(recipe(1, "Milk smoothie", 5, "Use milk.", "Blend milk with fruit.")),
            ingredients = listOf(ingredient(1, "milk", "milk", "DAIRY")),
            preferences = RecipeSuggestionPreferences(allergensToAvoid = setOf(RecipeAllergen.DAIRY)),
            today = today
        )

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun favoritesDietAndConfidenceExplainFit() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(
                food("Chicken breast", FoodCategory.MEAT, today.plusDays(2)),
                food("Rice", FoodCategory.PANTRY, today.plusMonths(2))
            ),
            recipes = listOf(recipe(1, "Chicken rice bowl", 15, "High protein rice bowl.", "Warm chicken and rice.")),
            ingredients = listOf(
                ingredient(1, "chicken", "chicken", "PROTEIN"),
                ingredient(1, "rice", "rice", "GRAIN")
            ),
            preferences = RecipeSuggestionPreferences(
                favoriteIngredients = setOf("chicken"),
                dietaryStyle = DietaryStyle.HIGH_PROTEIN,
                cookingConfidence = CookingConfidence.BEGINNER
            ),
            today = today
        )

        val reasons = suggestions.single().scoreReasons.joinToString(" ")
        assertTrue(reasons.contains("Uses ingredients you like"))
        assertTrue(reasons.contains("High protein"))
        assertTrue(reasons.contains("Beginner friendly"))
    }

    @Test
    fun dairyFreeStyleShowsNoDairyReason() {
        val suggestions = RecipeSuggestionEngine.buildSuggestions(
            foods = listOf(food("Rice", FoodCategory.PANTRY, today.plusMonths(2))),
            recipes = listOf(recipe(1, "Rice bowl", 10, "Simple rice bowl.", "Warm rice.")),
            ingredients = listOf(ingredient(1, "rice", "rice", "GRAIN")),
            preferences = RecipeSuggestionPreferences(dietaryStyle = DietaryStyle.DAIRY_FREE),
            today = today
        )

        assertTrue(suggestions.single().scoreReasons.any { it.contains("No dairy", ignoreCase = true) })
    }

    private fun food(name: String, category: FoodCategory, expires: LocalDate): FoodItemEntity =
        FoodItemEntity(
            id = name.hashCode().toLong(),
            name = name,
            category = category,
            location = FoodLocation.FRIDGE,
            expirationDate = expires,
            reminderDaysBefore = FreshnessCalculator.defaultReminderDays(category)
        )

    private fun recipe(id: Long, title: String, minutes: Int, description: String, steps: String): RecipeEntity =
        RecipeEntity(id = id, title = title, minutes = minutes, description = description, steps = steps, sourceName = "Test")

    private fun ingredient(
        recipeId: Long,
        label: String,
        keywords: String,
        category: String,
        required: Boolean = true
    ): RecipeIngredientEntity =
        RecipeIngredientEntity(recipeId = recipeId, label = label, keywords = keywords, category = category, required = required)
}
