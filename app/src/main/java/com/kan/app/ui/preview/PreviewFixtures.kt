package com.kan.app.ui.preview

import com.kan.app.core.LockScreenVisualization
import com.kan.app.core.LockTimerMode
import com.kan.app.core.OverlayStyle
import com.kan.app.data.ChallengeAttemptEntry
import com.kan.app.data.DailyHistoryEntry
import com.kan.app.data.PhoneSessionEntry
import com.kan.app.data.KanSnapshot
import java.time.LocalDate

internal object PreviewFixtures {
    val history: List<DailyHistoryEntry> = listOf(
        DailyHistoryEntry(
            date = LocalDate.of(2026, 5, 29),
            screenSeconds = 2L * 60 * 60 + 18 * 60,
            absenceSeconds = 9L * 60 * 60,
            peakAbsenceSeconds = 3L * 60 * 60 + 44 * 60,
            challengeSuccesses = 4,
            metBudget = true,
        ),
        DailyHistoryEntry(
            date = LocalDate.of(2026, 5, 28),
            screenSeconds = 4L * 60 * 60 + 6 * 60,
            absenceSeconds = 6L * 60 * 60,
            peakAbsenceSeconds = 2L * 60 * 60 + 12 * 60,
            challengeSuccesses = 2,
            metBudget = false,
        ),
        DailyHistoryEntry(
            date = LocalDate.of(2026, 5, 27),
            screenSeconds = 1L * 60 * 60 + 52 * 60,
            absenceSeconds = 10L * 60 * 60,
            peakAbsenceSeconds = 5L * 60 * 60 + 8 * 60,
            challengeSuccesses = 6,
            metBudget = true,
        ),
        DailyHistoryEntry(
            date = LocalDate.of(2026, 5, 26),
            screenSeconds = 2L * 60 * 60 + 40 * 60,
            absenceSeconds = 8L * 60 * 60,
            peakAbsenceSeconds = 4L * 60 * 60 + 35 * 60,
            challengeSuccesses = 3,
            metBudget = true,
        ),
    )

    val completedSnapshot = KanSnapshot(
        today = LocalDate.of(2026, 5, 29),
        dailyScreenSeconds = 2L * 60 * 60 + 18 * 60,
        dailyBudgetSeconds = 3L * 60 * 60,
        dailyBudgetStreak = 9,
        lockTimerMode = LockTimerMode.FullScreen,
        overlayStyle = OverlayStyle.Bar,
        overlayEnabled = true,
        lockScreenTimerEnabled = true,
        lockScreenVisualization = LockScreenVisualization.Arc,
        currentAbsenceStartedAtMillis = 0L,
        lastAbsenceSeconds = 48L * 60,
        allTimeAbsenceRecordSeconds = 7L * 60 * 60 + 12 * 60,
        dailyAbsenceSeconds = 4L * 60 * 60 + 5 * 60,
        dailyChallengeSuccesses = 4,
        challengeEndAtMillis = 0L,
        challengeDurationSeconds = 0L,
        challengeCompletedRecorded = false,
        overlayX = 96,
        overlayY = 240,
        history = history,
        phoneSessions = listOf(
            PhoneSessionEntry(1_779_996_600_000L, 1_779_997_260_000L),
            PhoneSessionEntry(1_779_978_800_000L, 1_779_979_520_000L),
        ),
        challengeAttempts = listOf(
            ChallengeAttemptEntry(1_779_982_400_000L, 1_779_984_200_000L, 1_800L, true),
            ChallengeAttemptEntry(1_779_974_400_000L, 1_779_974_760_000L, 1_200L, false),
        ),
        onboardingCompleted = true,
    )

    val onboardingSnapshot = completedSnapshot.copy(onboardingCompleted = false)
}
