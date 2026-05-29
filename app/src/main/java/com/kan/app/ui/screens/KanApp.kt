package com.kan.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kan.app.data.KanSnapshot
import com.kan.app.ui.theme.KanColors
import com.kan.app.ui.theme.KanTheme
import com.kan.app.ui.preview.PreviewFixtures

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
    onLockScreenTimerEnabledChanged: (Boolean) -> Unit,
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
                    buildStamp = buildStamp,
                )
                1 -> HistorySettingsScreen(
                    snapshot = snapshot,
                    onBudgetHoursChanged = onBudgetHoursChanged,
                    onOverlayEnabledChanged = onOverlayEnabledChanged,
                    onLockScreenTimerEnabledChanged = onLockScreenTimerEnabledChanged,
                )
            }
        }
    }
}

@Preview(
    name = "App shell · main flow",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun KanAppPreview() {
    KanTheme {
        KanApp(
            snapshot = PreviewFixtures.completedSnapshot,
            appName = "KAN",
            hasOverlayPermission = true,
            onRequestOverlayPermission = {},
            onRequestNotificationPermission = {},
            onFinishOnboarding = {},
            onBudgetHoursChanged = {},
            onOverlayEnabledChanged = {},
            onLockScreenTimerEnabledChanged = {},
            buildStamp = "preview v1.0 (1) · updated 2026-05-29 12:00:00",
        )
    }
}

@Preview(
    name = "App shell · onboarding",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun KanAppOnboardingPreview() {
    KanTheme {
        KanApp(
            snapshot = PreviewFixtures.onboardingSnapshot,
            appName = "KAN",
            hasOverlayPermission = false,
            onRequestOverlayPermission = {},
            onRequestNotificationPermission = {},
            onFinishOnboarding = {},
            onBudgetHoursChanged = {},
            onOverlayEnabledChanged = {},
            onLockScreenTimerEnabledChanged = {},
            buildStamp = "preview v1.0 (1) · updated 2026-05-29 12:00:00",
        )
    }
}
