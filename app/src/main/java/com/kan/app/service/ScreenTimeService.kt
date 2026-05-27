package com.kan.app.service

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.kan.app.core.LockTimerMode
import com.kan.app.data.KanSnapshot
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.ui.LockTimerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that observes screen on/off + keyguard transitions and drives:
 *   - Daily screen-time accumulation while the device is interactive
 *   - Absence (offline) timer while the device is off or locked
 *   - The floating overlay (via [OverlayController])
 *   - The persistent tracking notification (via [TrackingNotifications])
 *   - Optional lock-screen timer activity / heads-up banner
 */
class ScreenTimeService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: ScreenTimeRepository
    private lateinit var overlay: OverlayController
    private var activeTicker: Job? = null
    private var receiverRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> enterOfflineState()
                Intent.ACTION_SCREEN_ON -> if (isKeyguardLocked()) showLockedAbsenceState() else enterActiveState()
                Intent.ACTION_USER_PRESENT -> enterActiveState()
                Intent.ACTION_USER_UNLOCKED -> syncCurrentDeviceState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ScreenTimeRepository.get(this)
        overlay = OverlayController(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            onPositionChanged = repository::persistOverlayPosition,
        )
        TrackingNotifications.createChannel(this)
        registerScreenReceiver()
        postTrackingNotification()
        observeStyleChanges()
        syncCurrentDeviceState()
    }

    private fun observeStyleChanges() {
        serviceScope.launch {
            repository.snapshots
                .map { it.overlayStyle to it.overlayEnabled }
                .distinctUntilChanged()
                .onEach { refreshOverlay() }
                .collect {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> syncCurrentDeviceState()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        activeTicker?.cancel()
        overlay.remove()
        if (receiverRegistered) unregisterReceiver(screenStateReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- State transitions ---------------------------------------------------

    private fun syncCurrentDeviceState() {
        when {
            !isDeviceInteractive() -> enterOfflineState()
            isKeyguardLocked() -> showLockedAbsenceState()
            else -> enterActiveState()
        }
    }

    private fun enterActiveState() {
        repository.recoverActiveTimeOnRestart()
        val brokeRecord = repository.finishAbsence()
        postTrackingNotification()
        if (brokeRecord) {
            TrackingNotifications.notifyRecordIfAllowed(
                context = this,
                recordSeconds = repository.snapshots.value.allTimeAbsenceRecordSeconds,
            )
        }
        showOverlay()
        startActiveTicker()
    }

    private fun enterOfflineState() {
        stopActiveTicker()
        overlay.remove()
        repository.beginAbsence()
        postTrackingNotification()
    }

    private fun showLockedAbsenceState() {
        stopActiveTicker()
        overlay.remove()
        repository.ensureAbsenceStarted()
        when (repository.snapshots.value.lockTimerMode) {
            LockTimerMode.FullScreen -> {
                postTrackingNotification(asFullScreenIntent = true)
                launchLockScreenTimer()
            }
            LockTimerMode.Banner -> postAbsenceBannerNotification()
            LockTimerMode.Chronometer -> postTrackingNotification()
        }
    }

    private fun launchLockScreenTimer() {
        if (!isDeviceInteractive() || !isKeyguardLocked()) return
        startActivity(LockTimerActivity.createIntent(this))
    }

    // --- Ticker --------------------------------------------------------------

    private fun startActiveTicker() {
        if (activeTicker?.isActive == true) return
        activeTicker = serviceScope.launch {
            var lastTickElapsedRealtime = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val nowElapsedRealtime = SystemClock.elapsedRealtime()
                val rawElapsedSeconds = (nowElapsedRealtime - lastTickElapsedRealtime) / 1_000L
                // Cap the tick gap: large gaps indicate doze/sleep/missed screen-off broadcast and
                // should not be credited as active screen time. Also gate on isInteractive.
                val creditableSeconds = if (isDeviceInteractive()) {
                    rawElapsedSeconds.coerceAtMost(MAX_TICK_GAP_SECONDS)
                } else {
                    0L
                }
                if (creditableSeconds > 0L) {
                    repository.addActiveSeconds(creditableSeconds)
                } else {
                    repository.ensureCurrentDay()
                }
                // Advance the cursor by the full raw gap so we don't try to backfill the
                // un-creditable seconds on the next tick.
                if (rawElapsedSeconds > 0L) {
                    lastTickElapsedRealtime += rawElapsedSeconds * 1_000L
                }
                refreshOverlay()
            }
        }
    }

    private fun stopActiveTicker() {
        activeTicker?.cancel()
        activeTicker = null
        repository.markActiveTickerStopped()
    }

    // --- Overlay -------------------------------------------------------------

    private fun showOverlay() {
        val snapshot = repository.snapshots.value
        if (!snapshot.overlayEnabled) {
            overlay.remove()
            return
        }
        overlay.show(
            payload = snapshot.toOverlayPayload(),
            initialX = snapshot.overlayX,
            initialY = snapshot.overlayY,
        )
    }

    private fun refreshOverlay() {
        val snapshot = repository.snapshots.value
        if (!snapshot.overlayEnabled) {
            overlay.remove()
            return
        }
        overlay.update(snapshot.toOverlayPayload())
    }

    private fun KanSnapshot.toOverlayPayload(): OverlayController.Payload =
        OverlayController.Payload(
            seconds = dailyScreenSeconds,
            budgetSeconds = dailyBudgetSeconds,
            style = overlayStyle,
        )

    // --- Notifications -------------------------------------------------------

    private fun postTrackingNotification(asFullScreenIntent: Boolean = false) {
        val isAbsent = repository.snapshots.value.currentAbsenceStartedAtMillis > 0L
        val chronometerBaseMillis = System.currentTimeMillis() - repository.currentAbsenceElapsedMillis()
        startForeground(
            TrackingNotifications.ID_TRACKING,
            TrackingNotifications.buildTracking(
                context = this,
                isAbsent = isAbsent,
                chronometerBaseMillis = chronometerBaseMillis,
                fullScreenIntent = asFullScreenIntent,
            ),
        )
    }

    private fun postAbsenceBannerNotification() {
        val chronometerBaseMillis = System.currentTimeMillis() - repository.currentAbsenceElapsedMillis()
        startForeground(
            TrackingNotifications.ID_TRACKING,
            TrackingNotifications.buildAbsenceBanner(this, chronometerBaseMillis),
        )
    }

    // --- Device state helpers -----------------------------------------------

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_USER_UNLOCKED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun isDeviceInteractive(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive

    private fun isKeyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: false

    companion object {
        const val ACTION_REFRESH = "com.kan.app.action.REFRESH_TRACKING"
        const val ACTION_STOP = "com.kan.app.action.STOP_TRACKING"
        private const val TICK_INTERVAL_MILLIS = 1_000L
        private const val MAX_TICK_GAP_SECONDS = 5L

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeService::class.java).setAction(ACTION_REFRESH)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
