package com.kan.app.service

import android.Manifest
import android.app.Notification
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kan.app.LockTimerActivity
import com.kan.app.MainActivity
import com.kan.app.R
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.domain.toClockTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenTimeService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: ScreenTimeRepository
    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var activeTicker: Job? = null
    private var receiverRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> enterOfflineState()
                Intent.ACTION_SCREEN_ON -> if (isKeyguardLocked()) showLockedAbsenceState() else enterActiveState()
                Intent.ACTION_USER_PRESENT -> enterActiveState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ScreenTimeRepository.get(this)
        windowManager = getSystemService(WindowManager::class.java)
        createNotificationChannel()
        registerScreenReceiver()
        startForeground(NOTIFICATION_ID_TRACKING, buildTrackingNotification())
        syncCurrentDeviceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_REFRESH -> syncCurrentDeviceState()
            else -> syncCurrentDeviceState()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        activeTicker?.cancel()
        removeOverlay()
        if (receiverRegistered) unregisterReceiver(screenStateReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun syncCurrentDeviceState() {
        when {
            !isDeviceInteractive() -> enterOfflineState()
            isKeyguardLocked() -> showLockedAbsenceState()
            else -> enterActiveState()
        }
    }

    private fun enterActiveState() {
        val brokeRecord = repository.finishAbsence()
        updateTrackingNotification()
        if (brokeRecord) showRecordNotification()
        ensureOverlay()
        startActiveTicker()
    }

    private fun enterOfflineState() {
        activeTicker?.cancel()
        activeTicker = null
        updateOverlayText()
        removeOverlay()
        repository.beginAbsence()
        updateTrackingNotification()
    }

    private fun showLockedAbsenceState() {
        activeTicker?.cancel()
        activeTicker = null
        updateOverlayText()
        removeOverlay()
        repository.ensureAbsenceStarted()
        when (repository.snapshots.value.lockTimerMode) {
            ScreenTimeRepository.LOCK_TIMER_MODE_FULL_SCREEN -> {
                updateTrackingNotification()
                launchLockScreenTimer()
            }
            ScreenTimeRepository.LOCK_TIMER_MODE_BANNER -> showAbsenceBannerNotification()
            else -> updateTrackingNotification()
        }
    }

    private fun launchLockScreenTimer() {
        if (!isDeviceInteractive() || !isKeyguardLocked()) return
        startActivity(LockTimerActivity.createIntent(this))
    }

    private fun startActiveTicker() {
        if (activeTicker?.isActive == true) return
        activeTicker = serviceScope.launch {
            var lastTickMillis = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1_000L)
                val nowMillis = SystemClock.elapsedRealtime()
                val elapsedSeconds = (nowMillis - lastTickMillis) / 1_000L
                if (elapsedSeconds > 0L) {
                    repository.addActiveSeconds(elapsedSeconds)
                    lastTickMillis += elapsedSeconds * 1_000L
                } else {
                    repository.ensureCurrentDay()
                }
                updateOverlayText()
            }
        }
    }

    private fun ensureOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) {
            updateOverlayText()
            return
        }

        val snapshot = repository.snapshots.value
        val textView = TextView(this).apply {
            text = snapshot.dailyScreenSeconds.toClockTime()
            setTextColor(Color.rgb(18, 18, 18))
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(190, 248, 244, 237))
                setStroke(dp(1), Color.argb(68, 15, 15, 15))
            }
            elevation = dp(10).toFloat()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = snapshot.overlayX
            y = snapshot.overlayY
        }

        textView.installDragBehavior(params)
        overlayView = textView
        overlayParams = params
        windowManager.addView(textView, params)
    }

    private fun TextView.installDragBehavior(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = (initialY + (event.rawY - initialTouchY).toInt()).coerceAt(0)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    snapOverlayToNearestEdge(view, params)
                    repository.persistOverlayPosition(params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    private fun snapOverlayToNearestEdge(view: View, params: WindowManager.LayoutParams) {
        val bounds = currentWindowBounds()
        val maxX = (bounds.width() - view.width).coerceAtLeast(0)
        val maxY = (bounds.height() - view.height).coerceAtLeast(0)
        params.x = if (params.x + view.width / 2 < bounds.width() / 2) 0 else maxX
        params.y = params.y.coerceIn(0, maxY)
        windowManager.updateViewLayout(view, params)
    }

    private fun updateOverlayText() {
        overlayView?.text = repository.snapshots.value.dailyScreenSeconds.toClockTime()
    }

    private fun removeOverlay() {
        overlayView?.let { view -> windowManager.removeView(view) }
        overlayView = null
        overlayParams = null
    }

    private fun updateTrackingNotification() {
        startForeground(NOTIFICATION_ID_TRACKING, buildTrackingNotification())
    }

    private fun buildTrackingNotification(): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val overlaySettingsIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val snapshot = repository.snapshots.value
        val isAbsent = snapshot.currentAbsenceStartedAtMillis > 0L
        val absenceElapsedMillis = repository.currentAbsenceElapsedMillis()
        val chronometerWhenMillis = System.currentTimeMillis() - absenceElapsedMillis

        return NotificationCompat.Builder(this, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_kan_notification)
            .setContentTitle(if (isAbsent) "Absence time" else "KAN is here")
            .setContentText(
                if (isAbsent) {
                    "Live absence timer while the phone is locked."
                } else {
                    "Tracking active screen time and offline presence."
                },
            )
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .setWhen(chronometerWhenMillis)
            .setShowWhen(isAbsent)
            .setUsesChronometer(isAbsent)
            .setChronometerCountDown(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(
                if (isAbsent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW,
            )
            .addAction(0, "Overlay permission", overlaySettingsIntent)
            .build()
    }


    private fun showAbsenceBannerNotification() {
        val launchIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val absenceSeconds = (repository.currentAbsenceElapsedMillis() / 1_000L).coerceAtLeast(0L)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_kan_notification)
            .setContentTitle("Absence Time: ${absenceSeconds.toClockTime()}")
            .setContentText("Heads-up mode test while locked.")
            .setContentIntent(launchIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .build()
        startForeground(NOTIFICATION_ID_TRACKING, notification)
    }

    private fun showRecordNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val record = repository.snapshots.value.allTimeAbsenceRecordSeconds
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_kan_notification)
            .setContentTitle("New presence record")
            .setContentText("You stayed offline for ${record.toClockTime()}.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_RECORD, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID_TRACKING,
            getString(R.string.screen_time_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.screen_time_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun currentWindowBounds(): Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds
    } else {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun isDeviceInteractive(): Boolean = getSystemService(PowerManager::class.java).isInteractive

    private fun isKeyguardLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return keyguardManager?.isKeyguardLocked ?: false
    }

    private fun Int.coerceAt(minimum: Int): Int = coerceAtLeast(minimum)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_REFRESH = "com.kan.app.action.REFRESH_TRACKING"
        const val ACTION_STOP = "com.kan.app.action.STOP_TRACKING"
        private const val CHANNEL_ID_TRACKING = "kan_tracking"
        private const val NOTIFICATION_ID_TRACKING = 1001
        private const val NOTIFICATION_ID_RECORD = 1002

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeService::class.java).setAction(ACTION_REFRESH)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
