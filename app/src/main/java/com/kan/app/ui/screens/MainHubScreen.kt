package com.kan.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.core.LockTimerMode
import com.kan.app.data.KanSnapshot
import com.kan.app.domain.toClockTime
import com.kan.app.domain.toHumanDuration
import com.kan.app.ui.components.CeremonialMetric
import com.kan.app.ui.components.FloatingPanel
import com.kan.app.ui.components.GuardingScaffold
import com.kan.app.ui.components.SectionLabel
import com.kan.app.ui.theme.KanColors

@Composable
fun MainHubScreen(
    snapshot: KanSnapshot,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onLockTimerModeChanged: (LockTimerMode) -> Unit,
    buildStamp: String,
) {
    GuardingScaffold {
        Header()

        Spacer(Modifier.height(48.dp))

        FloatingPanel {
            CeremonialMetric(
                label = "SCREEN TIME",
                primary = snapshot.dailyScreenSeconds.toClockTime(),
                secondary = "/ ${snapshot.dailyBudgetSeconds.toClockTime()}",
                support = "${snapshot.dailyBudgetStreak}-day streak; floating timer mirrors this",
            )
        }

        Spacer(Modifier.height(20.dp))

        FloatingPanel {
            CeremonialMetric(
                label = "ABSENCE TIME",
                primary = snapshot.allTimeAbsenceRecordSeconds.toHumanDuration(),
                secondary = "record",
                support = "Last ${snapshot.lastAbsenceSeconds.toHumanDuration()}; mode below controls lock-screen behavior",
            )
            Spacer(Modifier.height(24.dp))
            LockTimerModeSelector(
                selectedMode = snapshot.lockTimerMode,
                onModeSelected = onLockTimerModeChanged,
            )
        }

        if (!hasOverlayPermission) {
            Spacer(Modifier.height(20.dp))
            FloatingPanel {
                OverlayPermissionButton(onRequestOverlayPermission)
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(
            text = buildStamp,
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )

        Spacer(Modifier.height(40.dp))
        Text(
            text = "HISTORY  /  SETTINGS    →    SWIPE LEFT",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            text = "I  IN",
            fontSize = 56.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 8.sp,
            color = KanColors.TextPrimary,
            lineHeight = 56.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "GUARDING CIRCLE  ·  FOCUS PERIMETER",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.6.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun LockTimerModeSelector(
    selectedMode: LockTimerMode,
    onModeSelected: (LockTimerMode) -> Unit,
) {
    SectionLabel("LOCK-SCREEN TIMER MODE")
    Spacer(Modifier.height(12.dp))
    LockTimerModeOptions.forEach { option ->
        LockTimerModeOption(
            title = option.title,
            subtitle = option.subtitle,
            selected = selectedMode == option.mode,
            onSelect = { onModeSelected(option.mode) },
        )
    }
}

@Composable
private fun LockTimerModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = KanColors.Steel,
                unselectedColor = KanColors.TextTertiary,
            ),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KanColors.TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                color = KanColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun OverlayPermissionButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
    ) {
        Text(
            text = "GRANT FLOATING TIMER ACCESS",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.Steel,
        )
    }
}

private data class LockTimerModeOptionSpec(
    val mode: LockTimerMode,
    val title: String,
    val subtitle: String,
)

private val LockTimerModeOptions = listOf(
    LockTimerModeOptionSpec(
        mode = LockTimerMode.Chronometer,
        title = "Option A: Passive chronometer",
        subtitle = "Standard low-priority ongoing notification timer.",
    ),
    LockTimerModeOptionSpec(
        mode = LockTimerMode.FullScreen,
        title = "Option B: Full-screen takeover",
        subtitle = "Launches a large lock-screen timer activity.",
    ),
    LockTimerModeOptionSpec(
        mode = LockTimerMode.Banner,
        title = "Option C: Heads-up banner",
        subtitle = "Triggers a high-priority banner with live absence value.",
    ),
)
