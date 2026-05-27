package com.kan.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kan.app.R
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
    ) { /* result handled by onboarding flow */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScreenTimeRepository.get(this)
        if (repository.isOnboardingCompleted()) startTrackingService()

        setContent {
            val snapshot by repository.snapshots.collectAsStateWithLifecycle()
            KanTheme {
                KanApp(
                    snapshot = snapshot,
                    appName = getString(R.string.app_name),
                    hasOverlayPermission = Settings.canDrawOverlays(this),
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onFinishOnboarding = ::finishOnboarding,
                    onBudgetHoursChanged = repository::updateDailyBudgetHours,
                    onLockTimerModeChanged = repository::updateLockTimerMode,
                    onOverlayStyleChanged = repository::updateOverlayStyle,
                    onLockScreenVisualizationChanged = repository::updateLockScreenVisualization,
                    buildStamp = buildStamp(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (repository.isOnboardingCompleted()) startTrackingService()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
        )
    }

    private fun finishOnboarding() {
        repository.markOnboardingCompleted()
        startTrackingService()
    }

    private fun startTrackingService() {
        ScreenTimeService.start(this)
    }

    private fun buildStamp(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val installedAt = Instant.ofEpochMilli(packageInfo.lastUpdateTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val versionName = packageInfo.versionName ?: "unknown"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        return "debug v$versionName ($versionCode) · updated $installedAt"
    }
}
