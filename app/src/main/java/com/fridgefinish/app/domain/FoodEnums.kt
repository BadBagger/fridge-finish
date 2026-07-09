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
    FRIDGE("Main Fridge"),
    FREEZER("Freezer"),
    PANTRY("Pantry"),
    GARAGE_FREEZER("Garage Freezer"),
    MINI_FRIDGE("Mini Fridge"),
    OTHER("Other")
}

enum class FreshnessStatus(val label: String) {
    FRESH("Fresh for now"),
    EAT_SOON("Eat soon"),
    EXPIRES_TODAY("Expires today"),
    EXPIRED("Past date"),
    FINISHED("Finished")
}

enum class FoodItemState(val label: String) {
    FRESH("Fresh"),
    USE_SOON("Use soon"),
    EXPIRED("Expired"),
    QUESTIONABLE("Questionable"),
    SPOILED("Spoiled"),
    FROZEN("Frozen")
}

enum class IngredientCategory(val label: String) {
    PROTEIN("Protein"),
    VEGETABLE("Vegetable"),
    FRUIT("Fruit"),
    GRAIN("Grain"),
    DAIRY("Dairy"),
    SAUCE("Sauce"),
    CONDIMENT("Condiment"),
    SPICE("Spice"),
    SNACK("Snack"),
    LEFTOVER("Leftover"),
    FROZEN("Frozen"),
    CANNED("Canned"),
    BAKING("Baking"),
    DRINK("Drink"),
    OTHER("Other")
}

enum class FoodSafetyRiskLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}
