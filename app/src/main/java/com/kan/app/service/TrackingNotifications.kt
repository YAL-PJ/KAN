package com.kan.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kan.app.R
import com.kan.app.domain.toClockTime
import com.kan.app.ui.MainActivity

/**
 * Constants for the foreground tracking notification + record celebration notification.
 * Centralized here so [ScreenTimeService] doesn't need to know channel / id details.
 */
internal object TrackingNotifications {
    const val CHANNEL_ID = "kan_tracking"
    const val ID_TRACKING = 1001
    const val ID_RECORD = 1002

    private const val REQUEST_CODE_LAUNCH = 0
    private const val REQUEST_CODE_OVERLAY_SETTINGS = 1
    private const val REQUEST_CODE_BANNER_LAUNCH = 2

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.screen_time_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.screen_time_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun buildTracking(
        context: Context,
        isAbsent: Boolean,
        chronometerBaseMillis: Long,
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_kan_notification)
        .setContentTitle(if (isAbsent) "Absence time" else "KAN is here")
        .setContentText(
            if (isAbsent) {
                "Live absence timer while the phone is locked."
            } else {
                "Tracking active screen time and offline presence."
            },
        )
        .setContentIntent(launchIntent(context, REQUEST_CODE_LAUNCH))
        .setOngoing(true)
        .setWhen(chronometerBaseMillis)
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
        .addAction(0, "Overlay permission", overlaySettingsIntent(context))
        .build()

    fun buildAbsenceBanner(context: Context, absenceSeconds: Long): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kan_notification)
            .setContentTitle("Absence Time: ${absenceSeconds.toClockTime()}")
            .setContentText("Heads-up mode test while locked.")
            .setContentIntent(launchIntent(context, REQUEST_CODE_BANNER_LAUNCH))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .build()

    fun notifyRecordIfAllowed(context: Context, recordSeconds: Long) {
        if (!canPostNotifications(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kan_notification)
            .setContentTitle("New presence record")
            .setContentText("You stayed offline for ${recordSeconds.toClockTime()}.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_RECORD, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun launchIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun overlaySettingsIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_OVERLAY_SETTINGS,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
