package com.fridgefinish.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object FreshnessCalculator {
    fun defaultReminderDays(category: FoodCategory): Int = when (category) {
        FoodCategory.LEFTOVERS -> 2
        FoodCategory.MEAT -> 2
        FoodCategory.DAIRY -> 3
        FoodCategory.PRODUCE -> 3
        FoodCategory.FROZEN -> 7
        FoodCategory.PANTRY -> 14
        FoodCategory.DRINKS,
        FoodCategory.SNACKS,
        FoodCategory.CONDIMENTS,
        FoodCategory.OTHER -> 3
    }

    fun status(
        expirationDate: LocalDate,
        reminderDaysBefore: Int,
        isFinished: Boolean,
        today: LocalDate = LocalDate.now()
    ): FreshnessStatus {
        if (isFinished) return FreshnessStatus.FINISHED
        val daysRemaining = ChronoUnit.DAYS.between(today, expirationDate)
        return when {
            daysRemaining < 0 -> FreshnessStatus.EXPIRED
            daysRemaining == 0L -> FreshnessStatus.EXPIRES_TODAY
            daysRemaining <= reminderDaysBefore -> FreshnessStatus.EAT_SOON
            else -> FreshnessStatus.FRESH
        }
    }

    fun daysRemainingText(expirationDate: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, expirationDate)
        return when {
            days < 0 -> "${-days} days past"
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            else -> "$days days left"
        }
    }

    fun urgencyRank(
        expirationDate: LocalDate,
        reminderDaysBefore: Int,
        isFinished: Boolean,
        today: LocalDate = LocalDate.now()
    ): Int {
        val statusRank = when (status(expirationDate, reminderDaysBefore, isFinished, today)) {
            FreshnessStatus.EXPIRED -> 0
            FreshnessStatus.EXPIRES_TODAY -> 1
            FreshnessStatus.EAT_SOON -> 2
            FreshnessStatus.FRESH -> 3
            FreshnessStatus.FINISHED -> 4
        }
        val days = ChronoUnit.DAYS.between(today, expirationDate).coerceIn(-999, 999).toInt()
        return statusRank * 2_000 + days + 999
    }
}
