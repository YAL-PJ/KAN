package com.kan.app.data

import java.time.LocalDate

data class DailyHistoryEntry(
    val date: LocalDate,
    val screenSeconds: Long,
    val absenceSeconds: Long,
    val peakAbsenceSeconds: Long,
    val challengeSuccesses: Int,
    val metBudget: Boolean,
)
