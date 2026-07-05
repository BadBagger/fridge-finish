package com.fridgefinish.app.domain

enum class FoodCategory(val label: String) {
    LEFTOVERS("Leftovers"),
    MEAT("Meat"),
    DAIRY("Dairy"),
    PRODUCE("Produce"),
    FROZEN("Frozen"),
    DRINKS("Drinks"),
    SNACKS("Snacks"),
    CONDIMENTS("Condiments"),
    PANTRY("Pantry"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String): FoodCategory =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER
    }
}

enum class FoodLocation(val label: String) {
    FRIDGE("Fridge"),
    FREEZER("Freezer"),
    PANTRY("Pantry")
}

enum class FreshnessStatus(val label: String) {
    FRESH("Fresh for now"),
    EAT_SOON("Eat soon"),
    EXPIRES_TODAY("Expires today"),
    EXPIRED("Past date"),
    FINISHED("Finished")
}
