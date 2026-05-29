package com.kan.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.data.KanSnapshot
import com.kan.app.domain.toClockTime
import com.kan.app.domain.toHumanDuration
import com.kan.app.ui.components.CeremonialMetric
import com.kan.app.ui.components.FloatingPanel
import com.kan.app.ui.components.GuardingScaffold
import com.kan.app.ui.components.SectionLabel
import com.kan.app.ui.theme.KanColors
import com.kan.app.ui.theme.KanTheme
import com.kan.app.ui.preview.PreviewFixtures

@Composable
fun MainHubScreen(
    snapshot: KanSnapshot,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    buildStamp: String,
) {
    GuardingScaffold {
        Header()

        Spacer(Modifier.height(48.dp))

        FloatingPanel {
            CeremonialMetric(
                label = "PHONE TIME",
                primary = snapshot.dailyScreenSeconds.toClockTime(),
                secondary = "/ ${snapshot.dailyBudgetSeconds.toClockTime()}",
                support = "${snapshot.dailyBudgetStreak}-day streak; floating timer mirrors this",
            )
        }

        Spacer(Modifier.height(20.dp))

        FloatingPanel {
            CeremonialMetric(
                label = "TIME AWAY",
                primary = snapshot.allTimeAbsenceRecordSeconds.toHumanDuration(),
                secondary = "record",
                support = "Last ${snapshot.lastAbsenceSeconds.toHumanDuration()} away; lock timer is full-screen with notification fallback",
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("VISUAL STYLE")
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Bars across floating, lock-screen, and challenge timers.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = KanColors.TextSecondary,
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
private fun OverlayPermissionButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
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

@Preview(
    name = "Main hub · phone",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun MainHubScreenPreview() {
    KanTheme {
        MainHubScreen(
            snapshot = PreviewFixtures.completedSnapshot,
            hasOverlayPermission = true,
            onRequestOverlayPermission = {},
            buildStamp = "preview v1.0 (1) · updated 2026-05-29 12:00:00",
        )
    }
}

@Preview(
    name = "Main hub · permission prompt",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun MainHubScreenMissingOverlayPermissionPreview() {
    KanTheme {
        MainHubScreen(
            snapshot = PreviewFixtures.completedSnapshot.copy(overlayEnabled = false),
            hasOverlayPermission = false,
            onRequestOverlayPermission = {},
            buildStamp = "preview v1.0 (1) · updated 2026-05-29 12:00:00",
        )
    }
}
