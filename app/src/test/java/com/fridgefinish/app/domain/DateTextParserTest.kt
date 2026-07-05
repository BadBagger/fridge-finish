package com.fridgefinish.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateTextParserTest {
    private val today = LocalDate.of(2026, 7, 4)

    @Test
    fun parsesSellByBottleDate() {
        assertEquals(
            LocalDate.of(2026, 8, 25),
            DateTextParser.extractDate("SELL BY 08-25-26 524 05 UV-UG 147 CTR!", today)
        )
    }

    @Test
    fun parsesNoisyOcrBottleDate() {
        assertEquals(
            LocalDate.of(2026, 8, 25),
            DateTextParser.extractDate("SEIL BY O8 25 26", today)
        )
    }

    @Test
    fun parsesMonthNameDate() {
        assertEquals(
            LocalDate.of(2026, 8, 25),
            DateTextParser.extractDate("BEST BY AUG 25 2026", today)
        )
    }

    @Test
    fun parsesCompactDate() {
        assertEquals(
            LocalDate.of(2026, 8, 25),
            DateTextParser.extractDate("SELL BY 082526", today)
        )
    }

    @Test
    fun returnsMultipleCandidates() {
        assertEquals(
            listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 9, 1)),
            DateTextParser.extractCandidates("SELL BY 08-25-26 LOT 090126", today)
        )
    }

    @Test
    fun parsesNoSpaceDayMonthYear() {
        assertEquals(
            LocalDate.of(2025, 12, 9),
            DateTextParser.extractDate("BB 09DEC2025 10:43 B", today)
        )
    }

    @Test
    fun prefersExpirationDateBeforeLotCode() {
        assertEquals(
            LocalDate.of(2026, 8, 16),
            DateTextParser.extractDate("LOT 070126 EXP 08/16/26", today)
        )
    }

    @Test
    fun parsesCompactYearMonthDayAfterExp() {
        assertEquals(
            LocalDate.of(2026, 8, 16),
            DateTextParser.extractDate("EXP 260816 L4", today)
        )
    }

    @Test
    fun parsesAttachedMonthDayYear() {
        assertEquals(
            LocalDate.of(2025, 12, 9),
            DateTextParser.extractDate("BEST BY DEC0925", today)
        )
    }

    @Test
    fun parsesBestBeforeAbbreviation() {
        assertEquals(
            LocalDate.of(2025, 12, 9),
            DateTextParser.extractDate("B/B 09 DEC 25", today)
        )
    }
}
