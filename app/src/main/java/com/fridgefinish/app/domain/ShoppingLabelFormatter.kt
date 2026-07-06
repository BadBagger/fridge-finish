package com.fridgefinish.app.domain

fun String.cleanShoppingName(): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return normalized
    val specific = when (normalized.lowercase()) {
        "produce", "vegetables" -> "spinach, peppers, or carrots"
        "fruit" -> "berries or bananas"
        "dairy" -> "milk, yogurt, or cheese"
        "meat", "protein" -> "chicken, eggs, or tofu"
        "pantry" -> "rice, pasta, or beans"
        "snacks" -> "crackers, pretzels, or nuts"
        "condiments" -> "sauce or dressing"
        "frozen" -> "frozen fruit or vegetables"
        else -> normalized
    }
    return specific.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
}
