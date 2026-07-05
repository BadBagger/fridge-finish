package com.fridgefinish.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class RecipeWithIngredients(
    val recipe: RecipeEntity,
    val ingredients: List<RecipeIngredientEntity>
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe_ingredients ORDER BY recipeId ASC, id ASC")
    fun observeIngredients(): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun countRecipes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients")
    suspend fun clearIngredients()

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()
}
