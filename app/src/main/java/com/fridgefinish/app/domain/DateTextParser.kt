package com.fridgefinish.app.domain

import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month

object DateTextParser {
    fun extractDate(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
        return extractCandidates(text, today).firstOrNull()
    }

    fun extractCandidates(text: String, today: LocalDate = LocalDate.now()): List<LocalDate> {
        val normalized = text
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        val dateish = normalized
            .replace('O', '0')
            .replace('o', '0')
            .replace('|', '1')
            .replace(Regex("(?i)bbd?"), "best by")
            .replace(Regex("(?i)b/b"), "best by")
            .replace(Regex("(?i)best\\s*before"), "best by")
            .replace(Regex("(?i)best\\s*by"), "best by")
            .replace(Regex("(?i)sell\\s*by"), "sell by")
            .replace(Regex("(?i)seil\\s*by"), "sell by")
            .replace(Regex("(?i)use\\s*by"), "use by")
            .replace(Regex("(?i)exp(?:ires|iration)?"), "exp")

        return buildList {
            addAll(parseIsoDates(dateish, today))
            addAll(parseNumericDates(dateish, today))
            addAll(parseCompactDates(dateish, today))
            addAll(parseMonthNameDates(dateish, today))
        }
            .groupBy { it.date }
            .map { (_, candidates) -> candidates.minWith(compareBy<Candidate> { it.rank }.thenBy { it.index }) }
            .sortedWith(compareBy<Candidate> { it.rank }.thenBy { it.index }.thenBy { it.date })
            .map { it.date }
    }

    private data class Candidate(val date: LocalDate, val rank: Int, val index: Int)

    private fun parseIsoDates(text: String, today: LocalDate): List<Candidate> = buildList {
        Regex("""(?<!\d)(20\d{2})[-/.:\s](\d{1,2})[-/.:\s](\d{1,2})(?!\d)""")
            .findAll(text)
            .forEach { match ->
                makeDate(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt()
                )?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
    }

    private fun parseNumericDates(text: String, today: LocalDate): List<Candidate> {
        val fullDates = buildList {
            Regex("""(?<!\d)(\d{1,2})[-/.:\s](\d{1,2})[-/.:\s](\d{2,4})(?!\d)""")
                .findAll(text)
                .forEach { match ->
                    val first = match.groupValues[1].toInt()
                    val second = match.groupValues[2].toInt()
                    val year = normalizeYear(match.groupValues[3].toInt())
                    val month = if (first > 12 && second <= 12) second else first
                    val day = if (first > 12 && second <= 12) first else second
                    makeDate(year, month, day)?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
                }
        }
        if (fullDates.isNotEmpty()) return fullDates

        return buildList {
            Regex("""(?i)(?:exp|expires|best by|use by|sell by)\D{0,12}(\d{1,2})[-/.:\s](\d{2,4})(?![-/.:\s]*\d)""")
                .findAll(text)
                .forEach { match ->
                    val month = match.groupValues[1].toInt()
                    val year = normalizeYear(match.groupValues[2].toInt())
                    makeDate(year, month, 1)?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
                }
        }
    }

    private fun parseCompactDates(text: String, today: LocalDate): List<Candidate> = buildList {
        Regex("""(?i)(?:exp|expires|best by|use by|sell by|by)?\D{0,12}(?<!\d)(\d{6}|\d{8})(?!\d)""")
            .findAll(text)
            .forEach { match ->
                val value = match.groupValues[1]
                val month = value.substring(0, 2).toIntOrNull() ?: return@forEach
                val day = value.substring(2, 4).toIntOrNull() ?: return@forEach
                val year = normalizeYear(value.substring(4).toIntOrNull() ?: return@forEach)
                makeDate(year, month, day)?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
        Regex("""(?i)(?:exp|best by|use by|sell by|by)\D{0,12}(?<!\d)(\d{2})(\d{2})(\d{2})(?!\d)""")
            .findAll(text)
            .forEach { match ->
                val year = normalizeYear(match.groupValues[1].toIntOrNull() ?: return@forEach)
                val month = match.groupValues[2].toIntOrNull() ?: return@forEach
                val day = match.groupValues[3].toIntOrNull() ?: return@forEach
                makeDate(year, month, day)?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
    }

    private fun parseMonthNameDates(text: String, today: LocalDate): List<Candidate> = buildList {
        val monthNames = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
        Regex("""(?i)\b$monthNames\.?\s+(\d{1,2}),?\s+(20\d{2}|\d{2})\b""")
            .findAll(text)
            .forEach { match ->
                makeDate(
                    normalizeYear(match.groupValues[3].toInt()),
                    monthFromName(match.groupValues[1]).value,
                    match.groupValues[2].toInt()
                )?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
        Regex("""(?i)\b(\d{1,2})\s+$monthNames\.?\s+(20\d{2}|\d{2})\b""")
            .findAll(text)
            .forEach { match ->
                makeDate(
                    normalizeYear(match.groupValues[3].toInt()),
                    monthFromName(match.groupValues[2]).value,
                    match.groupValues[1].toInt()
                )?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
        Regex("""(?i)\b(\d{1,2})\s*$monthNames\.?\s*(20\d{2}|\d{2})\b""")
            .findAll(text)
            .forEach { match ->
                makeDate(
                    normalizeYear(match.groupValues[3].toInt()),
                    monthFromName(match.groupValues[2]).value,
                    match.groupValues[1].toInt()
                )?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
        Regex("""(?i)\b$monthNames\.?\s*(\d{1,2})\s*(20\d{2}|\d{2})\b""")
            .findAll(text)
            .forEach { match ->
                makeDate(
                    normalizeYear(match.groupValues[3].toInt()),
                    monthFromName(match.groupValues[1]).value,
                    match.groupValues[2].toInt()
                )?.takeIf { plausible(it, today) }?.let { add(Candidate(it, rankFor(text, match.range.first), match.range.first)) }
            }
    }

    private fun rankFor(text: String, index: Int): Int {
        val before = text.take(index).takeLast(24).lowercase()
        return when {
            Regex("""(exp|best by|use by|sell by|bb|by)\W*$""").containsMatchIn(before) -> 0
            Regex("""(exp|best by|use by|sell by|bb|by)""").containsMatchIn(before) -> 1
            Regex("""(lot|code|batch|ctr|uv|ug)\W*$""").containsMatchIn(before) -> 5
            else -> 3
        }
    }

    private fun plausible(date: LocalDate, today: LocalDate): Boolean =
        date >= today.minusYears(1) && date <= today.plusYears(10)

    private fun monthFromName(value: String): Month = when (value.take(3).lowercase()) {
        "jan" -> Month.JANUARY
        "feb" -> Month.FEBRUARY
        "mar" -> Month.MARCH
        "apr" -> Month.APRIL
        "may" -> Month.MAY
        "jun" -> Month.JUNE
        "jul" -> Month.JULY
        "aug" -> Month.AUGUST
        "sep" -> Month.SEPTEMBER
        "oct" -> Month.OCTOBER
        "nov" -> Month.NOVEMBER
        else -> Month.DECEMBER
    }

    private fun normalizeYear(value: Int): Int =
        if (value < 100) 2000 + value else value

    private fun makeDate(year: Int, month: Int, day: Int): LocalDate? =
        try {
            LocalDate.of(year, month, day)
        } catch (_: DateTimeException) {
            null
        }
}
