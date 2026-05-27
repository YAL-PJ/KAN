package com.kan.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
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

    fun addActiveSeconds(seconds: Long, nowMillis: Long = System.currentTimeMillis()) {
        val safeSeconds = seconds.coerceAtLeast(0L)
        if (safeSeconds == 0L) return

        ensureCurrentDay(nowMillis)
        prefs.edit()
            .putLong(KEY_DAILY_SCREEN_SECONDS, prefs.getLong(KEY_DAILY_SCREEN_SECONDS, 0L) + safeSeconds)
            .applyAndPublish()
    }

    fun beginAbsence(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        ensureCurrentDay(nowMillis)
        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .applyAndPublish()
    }

    fun ensureAbsenceStarted(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        ensureCurrentDay(nowMillis)
        if (prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L) > 0L) return

        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, nowMillis)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, elapsedRealtimeMillis)
            .applyAndPublish()
    }

    fun finishAbsence(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Boolean {
        ensureCurrentDay(nowMillis)
        val startedAt = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L)
        if (startedAt <= 0L) return false

        val elapsedSeconds = (currentAbsenceElapsedMillis(nowMillis, elapsedRealtimeMillis) / 1_000L)
            .coerceAtLeast(0L)
        val dailyPeak = maxOf(prefs.getLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L), elapsedSeconds)
        val currentBest = prefs.getLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, 0L)
        val brokeRecord = elapsedSeconds > currentBest

        prefs.edit()
            .putLong(KEY_ABSENCE_STARTED_AT, 0L)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, 0L)
            .putLong(KEY_LAST_ABSENCE_SECONDS, elapsedSeconds)
            .putLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, dailyPeak)
            .putLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, maxOf(currentBest, elapsedSeconds))
            .applyAndPublish()

        return brokeRecord
    }

    fun currentAbsenceElapsedMillis(
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        val startedAt = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L)
        if (startedAt <= 0L) return 0L

        val startedAtElapsedRealtime = prefs.getLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, 0L)
        val elapsedRealtimeDelta = elapsedRealtimeMillis - startedAtElapsedRealtime
        val currentBootStartedAfterAbsence = nowMillis - elapsedRealtimeMillis > startedAt
        val elapsedMillis = if (
            startedAtElapsedRealtime > 0L &&
            elapsedRealtimeDelta >= 0L &&
            !currentBootStartedAfterAbsence
        ) {
            elapsedRealtimeDelta
        } else {
            nowMillis - startedAt
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

    fun ensureCurrentDay(nowMillis: Long = System.currentTimeMillis()) {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val storedDate = LocalDate.parse(prefs.getString(KEY_CURRENT_DATE, today.toString()) ?: today.toString())
        if (storedDate == today) return

        rollDayForward(storedDate, today)
    }

    private fun rollDayForward(storedDate: LocalDate, today: LocalDate) {
        val previousScreen = prefs.getLong(KEY_DAILY_SCREEN_SECONDS, 0L)
        val previousPeak = prefs.getLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L)
        val budget = prefs.getLong(KEY_DAILY_BUDGET_SECONDS, DEFAULT_DAILY_BUDGET_SECONDS)
        val metBudget = previousScreen <= budget
        val nextStreak = if (metBudget) prefs.getInt(KEY_DAILY_BUDGET_STREAK, 0) + 1 else 0
        val updatedHistory = listOf(
            DailyHistoryEntry(storedDate, previousScreen, previousPeak, metBudget),
        ) + readHistory()

        prefs.edit()
            .putString(KEY_CURRENT_DATE, today.toString())
            .putLong(KEY_DAILY_SCREEN_SECONDS, 0L)
            .putLong(KEY_DAILY_PEAK_ABSENCE_SECONDS, 0L)
            .putLong(KEY_ABSENCE_STARTED_AT, 0L)
            .putLong(KEY_ABSENCE_STARTED_AT_ELAPSED_REALTIME, 0L)
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
            currentAbsenceStartedAtMillis = prefs.getLong(KEY_ABSENCE_STARTED_AT, 0L),
            lastAbsenceSeconds = prefs.getLong(KEY_LAST_ABSENCE_SECONDS, 0L),
            allTimeAbsenceRecordSeconds = prefs.getLong(KEY_ALL_TIME_ABSENCE_RECORD_SECONDS, 0L),
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
        private const val KEY_LAST_ABSENCE_SECONDS = "last_absence_seconds"
        private const val KEY_ALL_TIME_ABSENCE_RECORD_SECONDS = "all_time_absence_record_seconds"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_HISTORY = "history"
        private const val KEY_LOCK_TIMER_MODE = "lock_timer_mode"
        private const val KEY_OVERLAY_STYLE = "overlay_style"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        private const val SECONDS_PER_HOUR = 3_600f
        private const val DEFAULT_DAILY_BUDGET_SECONDS = 2L * 60L * 60L
        private const val DEFAULT_OVERLAY_X = 0
        private const val DEFAULT_OVERLAY_Y = 240
        private const val HISTORY_LIMIT = 7

        const val MIN_BUDGET_HOURS = 0.5f
        const val MAX_BUDGET_HOURS = 12f

        @Volatile
        private var instance: ScreenTimeRepository? = null

        fun get(context: Context): ScreenTimeRepository = instance ?: synchronized(this) {
            instance ?: ScreenTimeRepository(context).also { instance = it }
        }
    }
}
