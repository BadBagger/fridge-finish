package com.fridgefinish.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "recipe_feedback")
data class RecipeFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeTitle: String,
    val mealFormat: String,
    val action: RecipeFeedbackAction,
    val ingredients: String = "",
    val createdAt: Instant = Instant.now()
)

enum class RecipeFeedbackAction {
    COOKED,
    SAVED,
    HIDDEN,
    TOO_MANY_MISSING,
    NOT_MY_TASTE,
    TOO_MUCH_WORK,
    BAD_SUGGESTION
}
