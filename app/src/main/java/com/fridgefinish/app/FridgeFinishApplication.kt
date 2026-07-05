package com.fridgefinish.app

import android.app.Application
import com.fridgefinish.app.data.FridgeFinishDatabase
import com.fridgefinish.app.data.FridgeFinishRepository
import com.fridgefinish.app.notifications.FoodNotificationScheduler

class FridgeFinishApplication : Application() {
    val repository: FridgeFinishRepository by lazy {
        val database = FridgeFinishDatabase.get(this)
        FridgeFinishRepository(
            foodDao = database.foodDao(),
            restockDao = database.restockDao(),
            recipeDao = database.recipeDao(),
            notifications = FoodNotificationScheduler(this)
        )
    }
}
