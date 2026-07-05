package com.fridgefinish.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodLocation
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val location: FoodLocation,
    val quantity: String? = null,
    val unit: String? = null,
    val purchaseDate: LocalDate? = null,
    val openedDate: LocalDate? = null,
    val expirationDate: LocalDate,
    val reminderDaysBefore: Int,
    val notes: String? = null,
    val imageUri: String? = null,
    val barcode: String? = null,
    val isFinished: Boolean = false,
    val finishedDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
