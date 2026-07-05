package com.fridgefinish.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FoodReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_FOOD_ID, -1)
        val name = intent.getStringExtra(EXTRA_FOOD_NAME).orEmpty()
        if (id > 0 && name.isNotBlank()) {
            context.showFoodReminder(id, name)
        }
    }

    companion object {
        const val EXTRA_FOOD_ID = "food_id"
        const val EXTRA_FOOD_NAME = "food_name"
    }
}
