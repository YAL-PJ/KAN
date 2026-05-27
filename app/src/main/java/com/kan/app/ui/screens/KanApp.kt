package com.kan.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kan.app.core.LockScreenVisualization
import com.kan.app.core.LockTimerMode
import com.kan.app.core.OverlayStyle
import com.kan.app.data.KanSnapshot
import com.kan.app.ui.theme.KanColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanApp(
    snapshot: KanSnapshot,
    appName: String,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onBudgetHoursChanged: (Float) -> Unit,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    onLockTimerModeChanged: (LockTimerMode) -> Unit,
    onOverlayStyleChanged: (OverlayStyle) -> Unit,
    onLockScreenVisualizationChanged: (LockScreenVisualization) -> Unit,
    buildStamp: String,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KanColors.Background,
        contentColor = KanColors.TextPrimary,
    ) {
        if (!snapshot.onboardingCompleted) {
            OnboardingScreen(
                appName = appName,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onFinish = onFinishOnboarding,
            )
            return@Surface
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> MainHubScreen(
                    snapshot = snapshot,
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onLockTimerModeChanged = onLockTimerModeChanged,
                    onOverlayStyleChanged = onOverlayStyleChanged,
                    onLockScreenVisualizationChanged = onLockScreenVisualizationChanged,
                    buildStamp = buildStamp,
                )
                1 -> HistorySettingsScreen(
                    snapshot = snapshot,
                    onBudgetHoursChanged = onBudgetHoursChanged,
                    onOverlayEnabledChanged = onOverlayEnabledChanged,
                )
            }
        }
    }
}
