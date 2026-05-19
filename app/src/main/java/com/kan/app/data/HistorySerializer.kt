package com.kan.app.data

import java.time.LocalDate

internal object HistorySerializer {
    private const val ENTRY_DELIMITER = '\n'
    private const val FIELD_DELIMITER = '|'
    private const val FIELD_COUNT = 4

    fun encode(entries: List<DailyHistoryEntry>, limit: Int): String = entries
        .take(limit)
        .joinToString(ENTRY_DELIMITER.toString()) { entry ->
            listOf(
                entry.date,
                entry.screenSeconds,
                entry.peakAbsenceSeconds,
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
        if (parts.size != FIELD_COUNT) return null
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
        return DailyHistoryEntry(
            date = date,
            screenSeconds = parts[1].toLongOrNull() ?: 0L,
            peakAbsenceSeconds = parts[2].toLongOrNull() ?: 0L,
            metBudget = parts[3].toBooleanStrictOrNull() ?: false,
        )
    }
}
