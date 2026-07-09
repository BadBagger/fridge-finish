package com.fridgefinish.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeFeedbackDao {
    @Query("SELECT * FROM recipe_feedback ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecipeFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: RecipeFeedbackEntity): Long

    @Delete
    suspend fun delete(feedback: RecipeFeedbackEntity)

    @Query("DELETE FROM recipe_feedback")
    suspend fun clearAll()

    @Query("DELETE FROM recipe_feedback WHERE recipeTitle = :title AND action = 'HIDDEN'")
    suspend fun clearHiddenForTitle(title: String)
}
