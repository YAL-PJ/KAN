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
import com.kan.app.core.LockTimerMode
import com.kan.app.data.KanSnapshot
import com.kan.app.ui.theme.KanColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanApp(
    snapshot: KanSnapshot,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onBudgetHoursChanged: (Float) -> Unit,
    onLockTimerModeChanged: (LockTimerMode) -> Unit,
    buildStamp: String,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KanColors.Background,
        contentColor = KanColors.TextPrimary,
    ) {
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
                    buildStamp = buildStamp,
                )
                1 -> HistorySettingsScreen(
                    snapshot = snapshot,
                    onBudgetHoursChanged = onBudgetHoursChanged,
                )
            }
        }
    }
}
