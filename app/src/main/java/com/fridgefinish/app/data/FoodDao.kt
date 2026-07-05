package com.fridgefinish.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fridgefinish.app.domain.FoodLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY expirationDate ASC")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE location = :location ORDER BY expirationDate ASC")
    fun observeByLocation(location: FoodLocation): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItemEntity): Long

    @Update
    suspend fun update(item: FoodItemEntity)

    @Delete
    suspend fun delete(item: FoodItemEntity)

    @Query("DELETE FROM food_items")
    suspend fun clear()
}
