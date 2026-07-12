package com.fridgefinish.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.FridgeFinishDatabase
import com.fridgefinish.app.data.RestockItemEntity
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodLocation
import com.fridgefinish.app.domain.FreshnessCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class MarketingDemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        runBlocking(Dispatchers.IO) {
            val database = FridgeFinishDatabase.get(context)
            database.foodDao().clear()
            database.restockDao().clear()
            val today = LocalDate.now()
            val foods = listOf(
                FoodItemEntity("Strawberries", FoodCategory.PRODUCE, FoodLocation.FRIDGE, "1", "container", today, 3),
                FoodItemEntity("Leftover pasta", FoodCategory.LEFTOVERS, FoodLocation.FRIDGE, "2", "servings", today, 2, isLeftover = true, sourceMeal = "Pasta dinner"),
                FoodItemEntity("Spinach", FoodCategory.PRODUCE, FoodLocation.FRIDGE, "1", "bag", today.plusDays(1), 3),
                FoodItemEntity("Chicken breast", FoodCategory.MEAT, FoodLocation.FRIDGE, "1.5", "lb", today.plusDays(2), 2),
                FoodItemEntity("Greek yogurt", FoodCategory.DAIRY, FoodLocation.FRIDGE, "4", "cups", today.plusDays(3), 3),
                FoodItemEntity("Bell peppers", FoodCategory.PRODUCE, FoodLocation.FRIDGE, "3", null, today.plusDays(4), 3),
                FoodItemEntity("Frozen vegetables", FoodCategory.FROZEN, FoodLocation.FREEZER, "1", "bag", today.plusDays(60), 7),
                FoodItemEntity("Pantry rice", FoodCategory.PANTRY, FoodLocation.PANTRY, "1", "bag", today.plusDays(180), 14),
                FoodItemEntity("Open pasta sauce", FoodCategory.CONDIMENTS, FoodLocation.FRIDGE, "1", "jar", today.plusDays(5), 3, isOpened = true),
                FoodItemEntity("Prepared lunch container", FoodCategory.LEFTOVERS, FoodLocation.FRIDGE, "1", "container", today.plusDays(1), 2, isLeftover = true)
            )
            foods.forEach { database.foodDao().insert(it) }
            database.restockDao().insert(RestockItemEntity(name = "Tortillas", quantity = "1 pack", note = "For leftover wraps", category = FoodCategory.PANTRY))
            database.restockDao().insert(RestockItemEntity(name = "Fresh berries", quantity = "1 pint", note = "For yogurt bowls", category = FoodCategory.PRODUCE))
        }
        pending.finish()
    }

    private fun FoodItemEntity(
        name: String,
        category: FoodCategory,
        location: FoodLocation,
        quantity: String?,
        unit: String?,
        expirationDate: LocalDate,
        reminderDaysBefore: Int,
        isOpened: Boolean = false,
        isLeftover: Boolean = false,
        sourceMeal: String? = null
    ) = FoodItemEntity(
        name = name,
        category = category,
        location = location,
        quantity = quantity,
        unit = unit,
        expirationDate = expirationDate,
        reminderDaysBefore = reminderDaysBefore.coerceAtLeast(FreshnessCalculator.defaultReminderDays(category).coerceAtMost(reminderDaysBefore)),
        isOpened = isOpened,
        isLeftover = isLeftover,
        sourceMeal = sourceMeal,
        dateCooked = if (isLeftover) expirationDate.minusDays(3) else null,
        notes = "Marketing demo item."
    )

    companion object {
        const val ACTION = "com.fridgefinish.app.DEBUG_MARKETING_DEMO"
    }
}
