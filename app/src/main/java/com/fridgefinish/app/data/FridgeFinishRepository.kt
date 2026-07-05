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
    private val notifications: FoodNotificationScheduler
) {
    val foods: Flow<List<FoodItemEntity>> = foodDao.observeAll()
    val restockItems: Flow<List<RestockItemEntity>> = restockDao.observeAll()

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
}
