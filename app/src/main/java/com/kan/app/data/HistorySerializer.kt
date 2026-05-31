package com.kan.app.data

import java.time.LocalDate

internal object HistorySerializer {
    private const val ENTRY_DELIMITER = '\n'
    private const val FIELD_DELIMITER = '|'
    private const val LEGACY_FIELD_COUNT = 4
    private const val PRE_ABSENCE_FIELD_COUNT = 5
    private const val FIELD_COUNT = 6

    fun encode(entries: List<DailyHistoryEntry>, limit: Int): String = entries
        .take(limit)
        .joinToString(ENTRY_DELIMITER.toString()) { entry ->
            listOf(
                entry.date,
                entry.screenSeconds,
                entry.absenceSeconds,
                entry.peakAbsenceSeconds,
                entry.challengeSuccesses,
                entry.metBudget,
            ).joinToString(FIELD_DELIMITER.toString())
        }

    fun decode(raw: String?, limit: Int): List<DailyHistoryEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(ENTRY_DELIMITER)
            .mapNotNull(::decodeEntry)
            .take(limit)
    }

    private fun decodeEntry(line: String): DailyHistoryEntry? {
        val parts = line.split(FIELD_DELIMITER)
        if (parts.size != FIELD_COUNT && parts.size != PRE_ABSENCE_FIELD_COUNT && parts.size != LEGACY_FIELD_COUNT) {
            return null
        }
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
        val absenceSeconds = if (parts.size == FIELD_COUNT) {
            parts[2].toLongOrNull() ?: 0L
        } else {
            0L
        }
        val peakAbsenceIndex = if (parts.size == FIELD_COUNT) 3 else 2
        val challengeSuccesses = when (parts.size) {
            FIELD_COUNT -> parts[4].toIntOrNull() ?: 0
            PRE_ABSENCE_FIELD_COUNT -> parts[3].toIntOrNull() ?: 0
            else -> 0
        }
        val metBudgetIndex = when (parts.size) {
            FIELD_COUNT -> 5
            PRE_ABSENCE_FIELD_COUNT -> 4
            else -> 3
        }
        return DailyHistoryEntry(
            date = date,
            screenSeconds = parts[1].toLongOrNull() ?: 0L,
            absenceSeconds = absenceSeconds,
            peakAbsenceSeconds = parts[peakAbsenceIndex].toLongOrNull() ?: 0L,
            challengeSuccesses = challengeSuccesses,
            metBudget = parts[metBudgetIndex].toBooleanStrictOrNull() ?: false,
        )
    }
}
