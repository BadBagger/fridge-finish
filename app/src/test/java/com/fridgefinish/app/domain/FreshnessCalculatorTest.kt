package com.fridgefinish.app.domain

import com.fridgefinish.app.notifications.NotificationDateCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FreshnessCalculatorTest {
    private val today = LocalDate.of(2026, 7, 4)

    @Test
    fun freshItem() {
        val status = FreshnessCalculator.status(
            expirationDate = today.plusDays(10),
            reminderDaysBefore = 3,
            isFinished = false,
            today = today
        )

        assertEquals(FreshnessStatus.FRESH, status)
    }

    @Test
    fun eatSoonItem() {
        val status = FreshnessCalculator.status(
            expirationDate = today.plusDays(2),
            reminderDaysBefore = 3,
            isFinished = false,
            today = today
        )

        assertEquals(FreshnessStatus.EAT_SOON, status)
    }

    @Test
    fun expiresTodayItem() {
        val status = FreshnessCalculator.status(
            expirationDate = today,
            reminderDaysBefore = 3,
            isFinished = false,
            today = today
        )

        assertEquals(FreshnessStatus.EXPIRES_TODAY, status)
    }

    @Test
    fun expiredItem() {
        val status = FreshnessCalculator.status(
            expirationDate = today.minusDays(1),
            reminderDaysBefore = 3,
            isFinished = false,
            today = today
        )

        assertEquals(FreshnessStatus.EXPIRED, status)
    }

    @Test
    fun finishedItemOverridesDate() {
        val status = FreshnessCalculator.status(
            expirationDate = today.minusDays(5),
            reminderDaysBefore = 3,
            isFinished = true,
            today = today
        )

        assertEquals(FreshnessStatus.FINISHED, status)
    }

    @Test
    fun defaultReminderDaysByCategory() {
        assertEquals(2, FreshnessCalculator.defaultReminderDays(FoodCategory.LEFTOVERS))
        assertEquals(2, FreshnessCalculator.defaultReminderDays(FoodCategory.MEAT))
        assertEquals(3, FreshnessCalculator.defaultReminderDays(FoodCategory.DAIRY))
        assertEquals(3, FreshnessCalculator.defaultReminderDays(FoodCategory.PRODUCE))
        assertEquals(7, FreshnessCalculator.defaultReminderDays(FoodCategory.FROZEN))
        assertEquals(14, FreshnessCalculator.defaultReminderDays(FoodCategory.PANTRY))
        assertEquals(3, FreshnessCalculator.defaultReminderDays(FoodCategory.OTHER))
    }

    @Test
    fun sortingByUrgency() {
        val dates = listOf(
            today.plusDays(12),
            today,
            today.minusDays(1),
            today.plusDays(2)
        )

        val sorted = dates.sortedBy {
            FreshnessCalculator.urgencyRank(
                expirationDate = it,
                reminderDaysBefore = 3,
                isFinished = false,
                today = today
            )
        }

        assertEquals(listOf(today.minusDays(1), today, today.plusDays(2), today.plusDays(12)), sorted)
    }

    @Test
    fun notificationDateCalculation() {
        val reminderDate = NotificationDateCalculator.reminderDate(
            expirationDate = today.plusDays(5),
            reminderDaysBefore = 2
        )

        assertEquals(today.plusDays(3), reminderDate)
    }
}
