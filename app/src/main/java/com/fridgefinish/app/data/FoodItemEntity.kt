package com.fridgefinish.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodItemState
import com.fridgefinish.app.domain.FoodSafetyRiskLevel
import com.fridgefinish.app.domain.IngredientCategory
import com.fridgefinish.app.domain.FoodLocation
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val ingredientCategory: IngredientCategory = IngredientCategory.OTHER,
    val subcategory: String? = null,
    val location: FoodLocation,
    val quantity: String? = null,
    val unit: String? = null,
    val purchaseDate: LocalDate? = null,
    val openedDate: LocalDate? = null,
    val dateCooked: LocalDate? = null,
    val expirationDate: LocalDate,
    val reminderDaysBefore: Int,
    val notes: String? = null,
    val sourceMeal: String? = null,
    val imageUri: String? = null,
    val barcode: String? = null,
    val isOpened: Boolean = false,
    val isLeftover: Boolean = false,
    val itemState: FoodItemState = FoodItemState.FRESH,
    val priorityScore: Int = 0,
    val safetyRiskLevel: FoodSafetyRiskLevel = FoodSafetyRiskLevel.LOW,
    val isFinished: Boolean = false,
    val finishedDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
