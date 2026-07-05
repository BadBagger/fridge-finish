package com.fridgefinish.app.notifications

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object NotificationDateCalculator {
    fun reminderDate(expirationDate: LocalDate, reminderDaysBefore: Int): LocalDate =
        expirationDate.minusDays(reminderDaysBefore.coerceAtLeast(0).toLong())

    fun reminderDateTime(
        expirationDate: LocalDate,
        reminderDaysBefore: Int,
        reminderTime: LocalTime = LocalTime.of(9, 0)
    ): LocalDateTime = LocalDateTime.of(reminderDate(expirationDate, reminderDaysBefore), reminderTime)
}
