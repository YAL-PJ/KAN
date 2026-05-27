package com.kan.app.data

import com.kan.app.core.LockTimerMode
import com.kan.app.core.OverlayStyle
import java.time.LocalDate

data class KanSnapshot(
    val today: LocalDate,
    val dailyScreenSeconds: Long,
    val dailyBudgetSeconds: Long,
    val dailyBudgetStreak: Int,
    val lockTimerMode: LockTimerMode,
    val overlayStyle: OverlayStyle,
    val currentAbsenceStartedAtMillis: Long,
    val lastAbsenceSeconds: Long,
    val allTimeAbsenceRecordSeconds: Long,
    val overlayX: Int,
    val overlayY: Int,
    val history: List<DailyHistoryEntry>,
    val onboardingCompleted: Boolean,
)
