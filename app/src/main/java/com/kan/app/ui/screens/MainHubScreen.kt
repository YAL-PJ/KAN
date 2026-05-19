package com.kan.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
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
import com.kan.app.ui.components.Hairline
import com.kan.app.ui.components.KanScaffold
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
    KanScaffold {
        Header()

        Spacer(Modifier.height(72.dp))

        CeremonialMetric(
            label = "SCREEN TIME",
            primary = snapshot.dailyScreenSeconds.toClockTime(),
            secondary = "/ ${snapshot.dailyBudgetSeconds.toClockTime()}",
            support = "${snapshot.dailyBudgetStreak}-day streak; floating timer mirrors this",
        )

        Spacer(Modifier.height(56.dp))
        Hairline()
        Spacer(Modifier.height(56.dp))

        CeremonialMetric(
            label = "ABSENCE TIME",
            primary = snapshot.allTimeAbsenceRecordSeconds.toHumanDuration(),
            secondary = "record",
            support = "Last ${snapshot.lastAbsenceSeconds.toHumanDuration()}; mode below controls lock-screen behavior",
        )

        Spacer(Modifier.height(34.dp))
        LockTimerModeSelector(
            selectedMode = snapshot.lockTimerMode,
            onModeSelected = onLockTimerModeChanged,
        )

        if (!hasOverlayPermission) {
            Spacer(Modifier.height(40.dp))
            OverlayPermissionButton(onRequestOverlayPermission)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = buildStamp,
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextMuted,
        )

        Spacer(Modifier.weight(1f))
        Text(
            text = "HISTORY / SETTINGS  —  SWIPE LEFT",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = KanColors.TextMuted,
        )
    }
}

@Composable
private fun Header() {
    Text(
        text = "KAN",
        fontSize = 42.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.8).sp,
        color = KanColors.TextPrimary,
    )
    Text(
        text = "DISCIPLINE DASHBOARD",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.4.sp,
        color = KanColors.TextTertiary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun LockTimerModeSelector(
    selectedMode: LockTimerMode,
    onModeSelected: (LockTimerMode) -> Unit,
) {
    SectionLabel("DEV: LOCK SCREEN TIMER MODE")
    Spacer(Modifier.height(10.dp))
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
        RadioButton(selected = selected, onClick = onSelect)
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
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Grant floating timer access",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = KanColors.Accent,
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
