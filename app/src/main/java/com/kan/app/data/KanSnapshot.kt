package com.kan.app.data

import java.time.LocalDate

data class DailyHistoryEntry(
    val date: LocalDate,
    val screenSeconds: Long,
    val peakAbsenceSeconds: Long,
    val metBudget: Boolean,
)

data class KanSnapshot(
    val today: LocalDate,
    val dailyScreenSeconds: Long,
    val dailyBudgetSeconds: Long,
    val dailyBudgetStreak: Int,
    val currentAbsenceStartedAtMillis: Long,
    val lastAbsenceSeconds: Long,
    val allTimeAbsenceRecordSeconds: Long,
    val overlayX: Int,
    val overlayY: Int,
    val history: List<DailyHistoryEntry>,
)
