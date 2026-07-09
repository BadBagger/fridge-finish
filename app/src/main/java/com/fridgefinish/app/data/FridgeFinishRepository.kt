package com.fridgefinish.app.data

import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodLocation
import com.fridgefinish.app.domain.FreshnessCalculator
import com.fridgefinish.app.notifications.FoodNotificationScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class FridgeFinishRepository(
    private val foodDao: FoodDao,
    private val restockDao: RestockDao,
    private val recipeDao: RecipeDao,
    private val notifications: FoodNotificationScheduler
) {
    val foods: Flow<List<FoodItemEntity>> = foodDao.observeAll()
    val restockItems: Flow<List<RestockItemEntity>> = restockDao.observeAll()
    val recipes: Flow<List<RecipeEntity>> = recipeDao.observeRecipes()
    val recipeIngredients: Flow<List<RecipeIngredientEntity>> = recipeDao.observeIngredients()

    fun foodsByLocation(location: FoodLocation): Flow<List<FoodItemEntity>> =
        foodDao.observeByLocation(location)

    suspend fun saveFood(item: FoodItemEntity): Long {
        val now = Instant.now()
        val id = if (item.id == 0L) {
            foodDao.insert(item.copy(createdAt = now, updatedAt = now))
        } else {
            foodDao.update(item.copy(updatedAt = now))
            item.id
        }
        val saved = foodDao.getById(id)
        if (saved != null && !saved.isFinished) notifications.schedule(saved) else notifications.cancel(id)
        return id
    }

    suspend fun deleteFood(item: FoodItemEntity) {
        foodDao.delete(item)
        notifications.cancel(item.id)
    }

    suspend fun markFinished(item: FoodItemEntity, addToRestock: Boolean) {
        val finished = item.copy(isFinished = true, finishedDate = LocalDate.now(), updatedAt = Instant.now())
        foodDao.update(finished)
        notifications.cancel(item.id)
        if (addToRestock) {
            restockDao.insert(
                RestockItemEntity(
                    name = item.name,
                    quantity = item.quantity,
                    note = "Replaces finished ${item.location.label} item.",
                    category = item.category
                )
            )
        }
    }

    suspend fun saveRestock(item: RestockItemEntity): Long {
        val now = Instant.now()
        return if (item.id == 0L) {
            restockDao.insert(item.copy(createdAt = now, updatedAt = now))
        } else {
            restockDao.update(item.copy(updatedAt = now))
            item.id
        }
    }

    suspend fun deleteRestock(item: RestockItemEntity) = restockDao.delete(item)

    suspend fun addSampleData() {
        val today = LocalDate.now()
        val samples = listOf(
            FoodItemEntity(name = "Chicken leftovers", category = FoodCategory.LEFTOVERS, location = FoodLocation.FRIDGE, expirationDate = today.plusDays(1), reminderDaysBefore = 2, notes = "Check before eating."),
            FoodItemEntity(name = "Milk", category = FoodCategory.DAIRY, location = FoodLocation.FRIDGE, expirationDate = today.plusDays(4), reminderDaysBefore = FreshnessCalculator.defaultReminderDays(FoodCategory.DAIRY)),
            FoodItemEntity(name = "Lettuce", category = FoodCategory.PRODUCE, location = FoodLocation.FRIDGE, expirationDate = today.plusDays(2), reminderDaysBefore = FreshnessCalculator.defaultReminderDays(FoodCategory.PRODUCE)),
            FoodItemEntity(name = "Frozen pizza", category = FoodCategory.FROZEN, location = FoodLocation.FREEZER, expirationDate = today.plusDays(90), reminderDaysBefore = FreshnessCalculator.defaultReminderDays(FoodCategory.FROZEN)),
            FoodItemEntity(name = "Rice", category = FoodCategory.PANTRY, location = FoodLocation.PANTRY, expirationDate = today.plusDays(180), reminderDaysBefore = FreshnessCalculator.defaultReminderDays(FoodCategory.PANTRY)),
            FoodItemEntity(name = "Yogurt", category = FoodCategory.DAIRY, location = FoodLocation.FRIDGE, expirationDate = today, reminderDaysBefore = FreshnessCalculator.defaultReminderDays(FoodCategory.DAIRY))
        )
        samples.forEach { saveFood(it) }
    }

    suspend fun seedRecipeDatabaseIfNeeded() {
        val existingTitles = recipeDao.recipeTitles().map { it.lowercase() }.toSet()
        builtInRecipes().filterNot { it.recipe.title.lowercase() in existingTitles }.forEach { seed ->
            val recipeId = recipeDao.insertRecipe(seed.recipe)
            recipeDao.insertIngredients(seed.ingredients.map { it.copy(recipeId = recipeId) })
        }
    }

    private data class SeedRecipe(
        val recipe: RecipeEntity,
        val ingredients: List<RecipeIngredientEntity>
    )

    private fun builtInRecipes(): List<SeedRecipe> = listOf(
        seedRecipe("Quick omelet", 15, "Use eggs with greens and cheese if you have it.", "Whisk eggs, cook with chopped greens or vegetables, then add cheese if available.", "eggs:egg:DAIRY", "spinach or peppers:spinach,tomato,pepper,onion,lettuce:PRODUCE", "shredded cheese:cheese:DAIRY:optional"),
        seedRecipe("Yogurt fruit bowl", 5, "A fast way to use fruit and yogurt.", "Spoon yogurt into a bowl, add fruit, then top with granola, cereal, or nuts if available.", "yogurt:yogurt:DAIRY", "berries or banana:berry,berries,banana,apple,strawberry,mango:PRODUCE", "granola or nuts:granola,cereal,nuts,cracker:SNACKS:optional"),
        seedRecipe("Leftover grain bowl", 12, "Use leftovers first with a pantry base.", "Warm leftovers with rice, pasta, or grains. Add greens or sauce if available.", "leftovers:leftover:LEFTOVERS", "rice or grain:rice,quinoa,pasta,noodle,pierogi:PANTRY", "spinach or peppers:spinach,tomato,pepper,onion,lettuce:PRODUCE:optional"),
        seedRecipe("Smoothie", 5, "Use fruit, dairy, and frozen items.", "Blend fruit with milk or yogurt. Add frozen fruit or ice if available.", "fruit:berry,banana,mango,strawberry:PRODUCE", "milk or yogurt:milk,yogurt:DAIRY", "frozen fruit or ice:frozen,ice:FROZEN:optional"),
        seedRecipe("Simple salad", 10, "A flexible way to finish greens and leftovers.", "Combine greens with protein or leftovers. Add dressing, sauce, or condiments.", "lettuce or spinach:lettuce,spinach,greens,salad:PRODUCE", "chicken, eggs, or tofu:chicken,egg,tofu,leftover:MEAT:optional", "salad dressing:dressing,sauce,condiment:CONDIMENTS:optional"),
        seedRecipe("Snack plate", 5, "Low-effort plate for partial items.", "Pair fruit or crunchy vegetables with dairy, protein, crackers, or snacks.", "apples, carrots, or berries:apple,carrot,celery,grape,berry:PRODUCE", "cheese, yogurt, or hummus:cheese,yogurt,egg,hummus:DAIRY:optional", "crackers or pretzels:cracker,chip,pretzel,nuts:SNACKS:optional"),
        seedRecipe("Pasta clean-out", 20, "Use pasta-style items with vegetables or leftovers.", "Cook pasta or prepared pasta-style items, then toss with vegetables, leftovers, and sauce or condiments.", "pasta:pasta,noodle,pierogi,ravioli:PANTRY", "tomatoes or spinach:tomato,spinach,pepper,onion,broccoli:PRODUCE", "pasta sauce:sauce,leftover,chicken,meat:CONDIMENTS:optional"),
        seedRecipe("Soup starter", 30, "Use broth, leftovers, vegetables, or pantry grains.", "Simmer vegetables and leftovers with broth or water. Add rice, pasta, or beans if available.", "carrots, celery, or onion:carrot,celery,onion,spinach,tomato:PRODUCE", "broth or beans:broth,stock,soup,bean,rice,pasta:PANTRY", "chicken, tofu, or leftovers:leftover,chicken,meat,tofu:MEAT:optional"),
        seedRecipe("Quesadilla", 12, "Use cheese, tortillas, and leftovers.", "Fill a tortilla with cheese and leftovers or vegetables, then toast until warm.", "tortillas:tortilla,wrap:PANTRY", "shredded cheese:cheese:DAIRY", "beans, peppers, or leftovers:leftover,chicken,pepper,onion,bean:LEFTOVERS:optional"),
        seedRecipe("Fried rice idea", 15, "Use rice, eggs, vegetables, and leftovers.", "Stir-fry rice with vegetables and egg or leftovers. Season with sauce or condiments.", "rice:rice:PANTRY", "eggs or leftover protein:egg,leftover,chicken,tofu:LEFTOVERS:optional", "peas, carrots, or peppers:pea,carrot,onion,pepper,spinach:PRODUCE"),
        seedRecipe("Microwave leftover plate", 8, "Fast leftover rescue when you do not want to cook.", "Reheat leftovers until hot, add a pantry side, then add fruit or vegetables if available.", "leftovers:leftover,chicken,meat,pasta:LEFTOVERS", "rice, bread, or crackers:rice,bread,cracker,tortilla:PANTRY:optional", "fruit or vegetables:apple,banana,carrot,lettuce,spinach,tomato:PRODUCE:optional"),
        seedRecipe("Air fryer freezer rescue", 18, "Use freezer items with a quick fresh side.", "Air fry frozen food until hot and crisp. Add salad greens, fruit, or yogurt on the side.", "frozen item:frozen,pizza,nugget,fries:FROZEN", "fresh side:lettuce,spinach,apple,banana,yogurt:PRODUCE:optional"),
        seedRecipe("Pantry bean tacos", 20, "Use pantry staples with produce and cheese.", "Warm beans or protein with tortillas, then top with cheese, peppers, lettuce, or salsa.", "beans or protein:bean,chicken,leftover,meat:PANTRY", "tortillas:tortilla,wrap:PANTRY", "toppings:cheese,lettuce,pepper,salsa,tomato:PRODUCE:optional")
    )

    private fun seedRecipe(
        title: String,
        minutes: Int,
        description: String,
        steps: String,
        vararg ingredientSpecs: String
    ): SeedRecipe =
        SeedRecipe(
            recipe = RecipeEntity(
                title = title,
                minutes = minutes,
                description = description,
                steps = steps,
                sourceName = "Fridge Finish recipe library",
                sourceUrl = null
            ),
            ingredients = ingredientSpecs.map { spec ->
                val parts = spec.split(":")
                RecipeIngredientEntity(
                    recipeId = 0,
                    label = parts[0],
                    keywords = parts.getOrElse(1) { parts[0] },
                    category = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() },
                    required = !parts.getOrNull(3).equals("optional", ignoreCase = true)
                )
            }
        )
}
