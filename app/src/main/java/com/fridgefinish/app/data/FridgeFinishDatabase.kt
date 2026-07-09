package com.fridgefinish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItemEntity::class, RestockItemEntity::class, RecipeEntity::class, RecipeIngredientEntity::class, RecipeFeedbackEntity::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FridgeFinishDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun restockDao(): RestockDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeFeedbackDao(): RecipeFeedbackDao

    companion object {
        @Volatile private var instance: FridgeFinishDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE restock_items ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN ingredientCategory TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE food_items ADD COLUMN subcategory TEXT")
                db.execSQL("ALTER TABLE food_items ADD COLUMN isOpened INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food_items ADD COLUMN isLeftover INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food_items ADD COLUMN priorityScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE food_items ADD COLUMN safetyRiskLevel TEXT NOT NULL DEFAULT 'LOW'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN itemState TEXT NOT NULL DEFAULT 'FRESH'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recipe_feedback (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeTitle TEXT NOT NULL,
                        mealFormat TEXT NOT NULL,
                        action TEXT NOT NULL,
                        ingredients TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN dateCooked INTEGER")
                db.execSQL("ALTER TABLE food_items ADD COLUMN sourceMeal TEXT")
            }
        }

        fun get(context: Context): FridgeFinishDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FridgeFinishDatabase::class.java,
                    "fridge_finish.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
