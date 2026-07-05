package com.fridgefinish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FoodItemEntity::class, RestockItemEntity::class, RecipeEntity::class, RecipeIngredientEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FridgeFinishDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun restockDao(): RestockDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: FridgeFinishDatabase? = null

        fun get(context: Context): FridgeFinishDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FridgeFinishDatabase::class.java,
                    "fridge_finish.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
