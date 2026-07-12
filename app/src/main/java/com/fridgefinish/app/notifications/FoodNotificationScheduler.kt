package com.fridgefinish.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fridgefinish.app.R
import com.fridgefinish.app.data.FoodItemEntity
import java.time.LocalDateTime
import java.time.ZoneId

class FoodNotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(item: FoodItemEntity) {
        if (!notificationsAllowed()) return
        createChannel()
        val reminderAt = NotificationDateCalculator.reminderDateTime(
            item.expirationDate,
            item.reminderDaysBefore
        )
        val triggerAt = reminderAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
            .coerceAtLeast(System.currentTimeMillis() + 5_000)

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(item.id, item.name, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        )
    }

    fun cancel(foodId: Long) {
        alarmManager.cancel(pendingIntent(foodId, "", PendingIntent.FLAG_NO_CREATE) ?: return)
    }

    fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Food reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders before food dates arrive"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun pendingIntent(foodId: Long, name: String, flags: Int): PendingIntent? {
        val intent = Intent(context, FoodReminderReceiver::class.java).apply {
            putExtra(FoodReminderReceiver.EXTRA_FOOD_ID, foodId)
            putExtra(FoodReminderReceiver.EXTRA_FOOD_NAME, name)
        }
        return PendingIntent.getBroadcast(
            context,
            foodId.toInt(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "food_reminders"
    }
}

fun Context.showFoodReminder(foodId: Long, foodName: String) {
    val scheduler = FoodNotificationScheduler(this)
    if (!scheduler.notificationsAllowed()) return
    scheduler.createChannel()
    val notification = NotificationCompat.Builder(this, FoodNotificationScheduler.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_fridge_finish)
        .setContentTitle("Eat soon")
        .setContentText("$foodName is coming up. Check before eating and use your judgment.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(this).notify(foodId.toInt(), notification)
}
