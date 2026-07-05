package com.fridgefinish.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockDao {
    @Query("SELECT * FROM restock_items ORDER BY isPurchased ASC, createdAt DESC")
    fun observeAll(): Flow<List<RestockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RestockItemEntity): Long

    @Update
    suspend fun update(item: RestockItemEntity)

    @Delete
    suspend fun delete(item: RestockItemEntity)

    @Query("DELETE FROM restock_items")
    suspend fun clear()
}
