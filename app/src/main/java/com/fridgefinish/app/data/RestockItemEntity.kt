package com.fridgefinish.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fridgefinish.app.domain.FoodCategory
import java.time.Instant

@Entity(tableName = "restock_items")
data class RestockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String? = null,
    val category: FoodCategory? = null,
    val isPurchased: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
