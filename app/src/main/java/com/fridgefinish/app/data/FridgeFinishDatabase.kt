package com.fridgefinish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItemEntity::class, RestockItemEntity::class, RecipeEntity::class, RecipeIngredientEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FridgeFinishDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun restockDao(): RestockDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: FridgeFinishDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restock_items ADD COLUMN note TEXT")
            }
        }

        fun get(context: Context): FridgeFinishDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FridgeFinishDatabase::class.java,
                    "fridge_finish.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
