package com.kan.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.kan.app.core.LockScreenVisualization
import com.kan.app.core.LockTimerMode
import com.kan.app.core.OverlayStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScreenTimeRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val snapshotFlow = MutableStateFlow(readSnapshot())

    val snapshots: StateFlow<KanSnapshot> = snapshotFlow.asStateFlow()

    fun addActiveSeconds(
        seconds: Long,
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        val safeSeconds = seconds.coerceAtLeast(0L)
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        val editor = prefs.edit()
            .putLong(KEY_LAST_ACTIVE_TICK_AT, nowMillis)
            .putLong(KEY_LAST_ACTIVE_TICK_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
        if (safeSeconds > 0L) {
            editor.putLong(
                KEY_DAILY_SCREEN_SECONDS,
                prefs.getLong(KEY_DAILY_SCREEN_SECONDS, 0L) + safeSeconds,
            )
        }
        editor.applyAndPublish()
    }

    /** Called when the active ticker stops cleanly (screen off). Prevents stale gap-credit on restart. */
    fun markActiveTickerStopped() {
        if (prefs.getLong(KEY_LAST_ACTIVE_TICK_AT, 0L) == 0L &&
            prefs.getLong(KEY_LAST_ACTIVE_TICK_AT_ELAPSED_REALTIME, 0L) == 0L
        ) return
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE_TICK_AT, 0L)
            .putLong(KEY_LAST_ACTIVE_TICK_AT_ELAPSED_REALTIME, 0L)
            .applyAndPublish()
    }

    /**
     * If the previous active ticker died without [markActiveTickerStopped] being called (e.g.
     * the service was killed), credit the time between the last tick and now — capped so we
     * never credit huge gaps that may include locked time we couldn't observe.
     */
    fun recoverActiveTimeOnRestart(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        val lastTickAt = prefs.getLong(KEY_LAST_ACTIVE_TICK_AT, 0L)
        if (lastTickAt <= 0L) return 0L
        val lastTickAtElapsedRealtime = prefs.getLong(KEY_LAST_ACTIVE_TICK_AT_ELAPSED_REALTIME, 0L)
        val gapMillis = computeDualClockElapsedMillis(
            startedAtMillis = lastTickAt,
            startedAtElapsedRealtime = lastTickAtElapsedRealtime,
            nowMillis = nowMillis,
            elapsedRealtimeMillis = elapsedRealtimeMillis,
        )
        val gapSeconds = (gapMillis / 1_000L).coerceIn(0L, MAX_RECOVERY_SECONDS)
        if (gapSeconds > 0L) {
            addActiveSeconds(gapSeconds, nowMillis, elapsedRealtimeMillis)
        } else {
            markActiveTickerStopped()
        }
        return gapSeconds
    }

    fun beginAbsence(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .applyAndPublish()
    }

    fun ensureAbsenceStarted(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        if (prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L) > 0L) return

        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .applyAndPublish()
    }

    fun finishAbsence(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Boolean {
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        val startedAt = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L)
        if (startedAt <= 0L) return false

        val sessionElapsedSeconds = (currentAbsenceElapsedMillis(nowMillis, elapsedRealtimeMillis) / 1_000L)
            .coerceAtLeast(0L)
        val daySegmentSeconds =
            (currentDaySegmentElapsedMillis(nowMillis, elapsedRealtimeMillis) / 1_000L)
                .coerceAtLeast(0L)
        val dailyPeak = maxOf(prefs.getLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L), daySegmentSeconds)
        val dailyTotal = prefs.getLong(KEY_DAILY_ABSENCE_SECONDS, 0L) + daySegmentSeconds
        val currentBest = prefs.getLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, 0L)
        val brokeRecord = sessionElapsedSeconds > currentBest

        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, 0L)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, 0L)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, 0L)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME, 0L)
            .putLong(KEY_LAST_ABSENCE_SECONDS, sessionElapsedSeconds)
            .putLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, dailyPeak)
            .putLong(KEY_DAILY_ABSENCE_SECONDS, dailyTotal)
            .putLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, maxOf(currentBest, sessionElapsedSeconds))
            .putLong(KEY_CHALLENGE_END_AT, 0L)
            .putLong(KEY_CHALLENGE_END_AT_ELAPSED_REALTIME, 0L)
            .putLong(KEY_CHALLENGE_DURATION_SECONDS, 0L)
            .applyAndPublish()

        return brokeRecord
    }

    fun startChallenge(
        durationSeconds: Long,
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        val safe = durationSeconds.coerceAtLeast(1L)
        val durationMillis = safe * 1_000L
        prefs.edit()
            .putLong(KEY_CHALLENGE_END_AT, nowMillis + durationMillis)
            .putLong(KEY_CHALLENGE_END_AT_ELAPSED_REALTIME, elapsedRealtimeMillis + durationMillis)
            .putLong(KEY_CHALLENGE_DURATION_SECONDS, safe)
            .applyAndPublish()
    }

    fun cancelChallenge() {
        prefs.edit()
            .putLong(KEY_CHALLENGE_END_AT, 0L)
            .putLong(KEY_CHALLENGE_END_AT_ELAPSED_REALTIME, 0L)
            .putLong(KEY_CHALLENGE_DURATION_SECONDS, 0L)
            .applyAndPublish()
    }

    fun updateLockScreenVisualization(visualization: LockScreenVisualization) {
        prefs.edit().putInt(KEY_LOCK_SCREEN_VISUALIZATION, visualization.storageValue).applyAndPublish()
    }

    fun currentAbsenceElapsedMillis(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        val startedAt = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L)
        if (startedAt <= 0L) return 0L
        val startedAtElapsedRealtime = prefs.getLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, 0L)
        return computeDualClockElapsedMillis(
            startedAtMillis = startedAt,
            startedAtElapsedRealtime = startedAtElapsedRealtime,
            nowMillis = nowMillis,
            elapsedRealtimeMillis = elapsedRealtimeMillis,
        )
    }

    fun currentChallengeRemainingMillis(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        val endAt = prefs.getLong(KEY_CHALLENGE_END_AT, 0L)
        if (endAt <= 0L) return 0L
        val endAtElapsedRealtime = prefs.getLong(KEY_CHALLENGE_END_AT_ELAPSED_REALTIME, 0L)
        val durationMillis = prefs.getLong(KEY_CHALLENGE_DURATION_SECONDS, 0L) * 1_000L
        val challengeStartedAt = endAt - durationMillis
        val currentBootStartedAfterChallenge =
            nowMillis - elapsedRealtimeMillis > challengeStartedAt
        val remaining = if (
            endAtElapsedRealtime > 0L &&
            !currentBootStartedAfterChallenge
        ) {
            endAtElapsedRealtime - elapsedRealtimeMillis
        } else {
            endAt - nowMillis
        }
        return remaining.coerceAtLeast(0L)
    }

    /** Today's-portion of an in-progress absence, for daily attribution. */
    private fun currentDaySegmentElapsedMillis(
        nowMillis: Long,
        elapsedRealtimeMillis: Long,
    ): Long {
        val startedAt = prefs.getLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, 0L)
        if (startedAt <= 0L) return 0L
        val startedAtElapsedRealtime =
            prefs.getLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME, 0L)
        return computeDualClockElapsedMillis(
            startedAtMillis = startedAt,
            startedAtElapsedRealtime = startedAtElapsedRealtime,
            nowMillis = nowMillis,
            elapsedRealtimeMillis = elapsedRealtimeMillis,
        )
    }

    fun currentDailyAbsenceSeconds(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        val stored = prefs.getLong(KEY_DAILY_ABSENCE_SECONDS, 0L)
        val ongoing = currentDaySegmentElapsedMillis(nowMillis, elapsedRealtimeMillis) / 1_000L
        return stored + ongoing.coerceAtLeast(0L)
    }

    fun currentDailyPeakAbsenceSeconds(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        ensureCurrentDay(nowMillis, elapsedRealtimeMillis)
        val stored = prefs.getLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L)
        val ongoing = currentDaySegmentElapsedMillis(nowMillis, elapsedRealtimeMillis) / 1_000L
        return maxOf(stored, ongoing.coerceAtLeast(0L))
    }

    private fun computeDualClockElapsedMillis(
        startedAtMillis: Long,
        startedAtElapsedRealtime: Long,
        nowMillis: Long,
        elapsedRealtimeMillis: Long,
    ): Long {
        val elapsedRealtimeDelta = elapsedRealtimeMillis - startedAtElapsedRealtime
        val currentBootStartedAfterEvent = nowMillis - elapsedRealtimeMillis > startedAtMillis
        val elapsedMillis = if (
            startedAtElapsedRealtime > 0L &&
            elapsedRealtimeDelta >= 0L &&
            !currentBootStartedAfterEvent
        ) {
            elapsedRealtimeDelta
        } else {
            nowMillis - startedAtMillis
        }
        return elapsedMillis.coerceAtLeast(0L)
    }

    fun persistOverlayPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_OVERLAY_X, x)
            .putInt(KEY_OVERLAY_Y, y)
            .applyAndPublish()
    }

    fun updateDailyBudgetHours(hours: Float) {
        val seconds = (hours.coerceIn(MIN_BUDGET_HOURS, MAX_BUDGET_HOURS) * SECONDS_PER_HOUR).toLong()
        prefs.edit().putLong(KEY_DAILY_BUDGET_SECONDS, seconds).applyAndPublish()
    }

    fun updateLockTimerMode(mode: LockTimerMode) {
        prefs.edit().putInt(KEY_LOCK_TIMER_MODE, mode.storageValue).applyAndPublish()
    }

    fun updateOverlayStyle(style: OverlayStyle) {
        prefs.edit().putInt(KEY_OVERLAY_STYLE, style.storageValue).applyAndPublish()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun markOnboardingCompleted() {
        if (prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) return
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).applyAndPublish()
    }

    fun ensureCurrentDay(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        var storedDate = LocalDate.parse(prefs.getString(KEY_CURRENT_DATE, today.toString()) ?: today.toString())
        if (!storedDate.isBefore(today)) return

        while (storedDate.isBefore(today)) {
            val nextDate = storedDate.plusDays(1)
            val nextDayStartMillis = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val nextDayStartElapsedRealtime =
                (elapsedRealtimeMillis - (nowMillis - nextDayStartMillis)).coerceAtLeast(0L)
            rollDayForward(storedDate, nextDate, nextDayStartMillis, nextDayStartElapsedRealtime)
            storedDate = nextDate
        }
    }

    private fun rollDayForward(
        storedDate: LocalDate,
        newDate: LocalDate,
        newDayStartMillis: Long,
        newDayStartElapsedRealtime: Long,
    ) {
        val previousScreen = prefs.getLong(KEY_DAILY_SCREEN_SECONDS, 0L)
        val storedPeak = prefs.getLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L)
        val budget = prefs.getLong(KEY_DAILY_BUDGET_SECONDS, DEFAULT_DAILY_BUDGET_SECONDS)

        val daySegmentStart = prefs.getLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, 0L)
        val preSegmentSeconds = if (daySegmentStart in 1L until newDayStartMillis) {
            ((newDayStartMillis - daySegmentStart) / 1_000L).coerceAtLeast(0L)
        } else {
            0L
        }
        val archivedPeak = maxOf(storedPeak, preSegmentSeconds)
        val metBudget = previousScreen <= budget
        val nextStreak = if (metBudget) prefs.getInt(KEY_DAILY_BUDGET_STREAK, 0) + 1 else 0
        val updatedHistory = listOf(
            DailyHistoryEntry(storedDate, previousScreen, archivedPeak, metBudget),
        ) + readHistory()

        val absenceOngoing = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L) > 0L
        val newDaySegmentStart = if (absenceOngoing) newDayStartMillis else 0L
        val newDaySegmentStartElapsedRealtime = if (absenceOngoing) newDayStartElapsedRealtime else 0L

        prefs.edit()
            .putString(KEY_CURRENT_DATE, newDate.toString())
            .putLong(KEY_DAILY_SCREEN_SECONDS, 0L)
            .putLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L)
            .putLong(KEY_DAILY_ABSENCE_SECONDS, 0L)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT, newDaySegmentStart)
            .putLong(KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME, newDaySegmentStartElapsedRealtime)
            .putInt(KEY_DAILY_BUDGET_STREAK, nextStreak)
            .putString(KEY_HISTORY, HistorySerializer.encode(updatedHistory, HISTORY_LIMIT))
            .applyAndPublish()
    }

    private fun readSnapshot(): KanSnapshot {
        val today = LocalDate.parse(prefs.getString(KEY_CURRENT_DATE, null) ?: LocalDate.now(zoneId).toString())
        return KanSnapshot(
            today = today,
            dailyScreenSeconds = prefs.getLong(KEY_DAILY_SCREEN_SECONDS, 0L),
            dailyBudgetSeconds = prefs.getLong(KEY_DAILY_BUDGET_SECONDS, DEFAULT_DAILY_BUDGET_SECONDS),
            dailyBudgetStreak = prefs.getInt(KEY_DAILY_BUDGET_STREAK, 0),
            lockTimerMode = LockTimerMode.fromStorageValue(prefs.getInt(KEY_LOCK_TIMER_MODE, 0)),
            overlayStyle = OverlayStyle.fromStorageValue(prefs.getInt(KEY_OVERLAY_STYLE, 0)),
            lockScreenVisualization = LockScreenVisualization.fromStorageValue(
                prefs.getInt(KEY_LOCK_SCREEN_VISUALIZATION, 0),
            ),
            currentAbsenceStartedAtMillis = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L),
            lastAbsenceSeconds = prefs.getLong(KEY_LAST_ABSENCE_SECONDS, 0L),
            allTimeAbsenceRecordSeconds = prefs.getLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, 0L),
            dailyAbsenceSeconds = prefs.getLong(KEY_DAILY_ABSENCE_SECONDS, 0L),
            challengeEndAtMillis = prefs.getLong(KEY_CHALLENGE_END_AT, 0L),
            challengeDurationSeconds = prefs.getLong(KEY_CHALLENGE_DURATION_SECONDS, 0L),
            overlayX = prefs.getInt(KEY_OVERLAY_X, DEFAULT_OVERLAY_X),
            overlayY = prefs.getInt(KEY_OVERLAY_Y, DEFAULT_OVERLAY_Y),
            history = readHistory(),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
        )
    }

    private fun readHistory(): List<DailyHistoryEntry> =
        HistorySerializer.decode(prefs.getString(KEY_HISTORY, null), HISTORY_LIMIT)

    private fun SharedPreferences.Editor.applyAndPublish() {
        apply()
        snapshotFlow.value = readSnapshot()
    }

    companion object {
        private const val PREFS_NAME = "kan_screen_time"
        private const val KEY_CURRENT_DATE = "current_date"
        private const val KEY_DAILY_SCREEN_SECONDS = "daily_screen_seconds"
        private const val KEY_DAILY_BUDGET_SECONDS = "daily_budget_seconds"
        private const val KEY_DAILY_BUDGET_STREAK = "daily_budget_streak"
        private const val KEY_DAILY_PEAK_ABSENCE_SECONDS = "daily_peak_absence_seconds"
        private const val KEY_ABSENCE_STARTED_AT = "absence_started_at"
        private const val KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME = "absence_started_at_elapsed_realtime"
        private const val KEY_ABSENCE_DAY_SEGMENT_STARTED_AT = "absence_day_segment_started_at"
        private const val KEY_ABSENCE_DAY_SEGMENT_STARTED_AT_ELAPSED_REALTIME =
            "absence_day_segment_started_at_elapsed_realtime"
        private const val KEY_LAST_ABSENCE_SECONDS = "last_absence_seconds"
        private const val KEY_ALL_TIME_ABSENCE_RECORD_SECONDS = "all_time_absence_record_seconds"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_HISTORY = "history"
        private const val KEY_LOCK_TIMER_MODE = "lock_timer_mode"
        private const val KEY_OVERLAY_STYLE = "overlay_style"
        private const val KEY_LOCK_SCREEN_VISUALIZATION = "lock_screen_visualization"
        private const val KEY_DAILY_ABSENCE_SECONDS = "daily_absence_seconds"
        private const val KEY_CHALLENGE_END_AT = "challenge_end_at"
        private const val KEY_CHALLENGE_END_AT_ELAPSED_REALTIME = "challenge_end_at_elapsed_realtime"
        private const val KEY_CHALLENGE_DURATION_SECONDS = "challenge_duration_seconds"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_ACTIVE_TICK_AT = "last_active_tick_at"
        private const val KEY_LAST_ACTIVE_TICK_AT_ELAPSED_REALTIME = "last_active_tick_at_elapsed_realtime"

        private const val SECONDS_PER_HOUR = 3_600f
        private const val DEFAULT_DAILY_BUDGET_SECONDS = 2L * 60L * 60L
        private const val DEFAULT_OVERLAY_X = 0
        private const val DEFAULT_OVERLAY_Y = 240
        private const val HISTORY_LIMIT = 7
        private const val MAX_RECOVERY_SECONDS = 60L

        const val MIN_BUDGET_HOURS = 0.5f
        const val MAX_BUDGET_HOURS = 12f

        @Volatile
        private var instance: ScreenTimeRepository? = null

        fun get(context: Context): ScreenTimeRepository = instance ?: synchronized(this) {
            instance ?: ScreenTimeRepository(context).also { instance = it }
        }
    }
}
