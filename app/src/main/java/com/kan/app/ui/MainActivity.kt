package com.kan.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kan.app.BuildConfig
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.service.ScreenTimeService
import com.kan.app.ui.screens.KanApp
import com.kan.app.ui.theme.KanTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var repository: ScreenTimeRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startTrackingService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScreenTimeRepository.get(this)
        requestNotificationPermissionIfNeeded()
        startTrackingService()

        setContent {
            val snapshot by repository.snapshots.collectAsStateWithLifecycle()
            KanTheme {
                KanApp(
                    snapshot = snapshot,
                    hasOverlayPermission = Settings.canDrawOverlays(this),
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onBudgetHoursChanged = repository::updateDailyBudgetHours,
                    onLockTimerModeChanged = repository::updateLockTimerMode,
                    buildStamp = buildStamp(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startTrackingService()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
        )
    }

    private fun startTrackingService() {
        ScreenTimeService.start(this)
    }

    private fun buildStamp(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val installedAt = Instant.ofEpochMilli(packageInfo.lastUpdateTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return "debug v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · updated $installedAt"
    }
}
