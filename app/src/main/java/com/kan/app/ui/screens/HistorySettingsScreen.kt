package com.kan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.data.DailyHistoryEntry
import com.kan.app.data.KanSnapshot
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.domain.toHumanDuration
import com.kan.app.ui.components.FloatingPanel
import com.kan.app.ui.components.GuardingScaffold
import com.kan.app.ui.components.Hairline
import com.kan.app.ui.components.SectionLabel
import com.kan.app.ui.theme.KanColors
import java.time.format.DateTimeFormatter

private const val SECONDS_PER_HOUR = 3_600f
private val WeekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

@Composable
fun HistorySettingsScreen(
    snapshot: KanSnapshot,
    onBudgetHoursChanged: (Float) -> Unit,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    onLockScreenTimerEnabledChanged: (Boolean) -> Unit,
) {
    val budgetHours = snapshot.dailyBudgetSeconds / SECONDS_PER_HOUR
    GuardingScaffold {
        Header()

        Spacer(Modifier.height(36.dp))

        FloatingPanel {
            SectionLabel("DAILY LEDGER")
            Spacer(Modifier.height(18.dp))
            TodayChallengeWins(snapshot.dailyChallengeSuccesses)
            if (snapshot.history.isNotEmpty()) {
                Hairline(alpha = 0.5f)
            }
            HistoryList(snapshot.history)
        }

        Spacer(Modifier.height(20.dp))

        FloatingPanel {
            BudgetSlider(
                budgetHours = budgetHours,
                onBudgetHoursChanged = onBudgetHoursChanged,
            )
        }
        Spacer(Modifier.height(20.dp))
        FloatingPanel {
            OverlayToggleRow(
                enabled = snapshot.overlayEnabled,
                onEnabledChanged = onOverlayEnabledChanged,
            )
        }
        Spacer(Modifier.height(20.dp))
        FloatingPanel {
            LockScreenTimerToggleRow(
                enabled = snapshot.lockScreenTimerEnabled,
                onEnabledChanged = onLockScreenTimerEnabledChanged,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "MAIN HUB    ←    SWIPE RIGHT",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun LockScreenTimerToggleRow(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    SectionLabel("LOCK-SCREEN TIMER")
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (enabled) "Full-screen before unlock" else "Notification fallback only",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
        )
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
}

@Composable
private fun OverlayToggleRow(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    SectionLabel("FLOATING TIMER")
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (enabled) "Always visible" else "Hidden",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
        )
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            text = "HISTORY",
            fontSize = 36.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 2.sp,
            color = KanColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "INSIDE THE CIRCLE  ·  WHAT THE PERIMETER HELD",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun TodayChallengeWins(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "TODAY",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextTertiary,
        )
        Text(
            text = "$count CHALLENGE WINS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.End,
            color = if (count > 0) KanColors.PrismGreen else KanColors.TextSecondary,
        )
    }
}

@Composable
private fun HistoryList(entries: List<DailyHistoryEntry>) {
    if (entries.isEmpty()) {
        Text(
            text = "Completed days will appear here below today's challenge wins.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
            lineHeight = 22.sp,
        )
        return
    }
    entries.forEachIndexed { index, entry ->
        HistoryRow(entry)
        if (index < entries.lastIndex) {
            Hairline(alpha = 0.5f)
        }
    }
}

@Composable
private fun HistoryRow(entry: DailyHistoryEntry) {
    val label = entry.date.format(WeekdayFormatter)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextTertiary,
        )
        Text(
            text = "${entry.screenSeconds.toHumanDuration()} PHONE  /  ${entry.peakAbsenceSeconds.toHumanDuration()} AWAY  /  ${entry.challengeSuccesses} WON${if (entry.metBudget) "  ✓" else ""}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.End,
            color = if (entry.metBudget) KanColors.TextPrimary else KanColors.TextSecondary,
        )
    }
}

@Composable
private fun BudgetSlider(
    budgetHours: Float,
    onBudgetHoursChanged: (Float) -> Unit,
) {
    SectionLabel("DAILY PHONE BUDGET")
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Limit",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
        )
        Text(
            text = "%.1f HRS".format(budgetHours),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            color = KanColors.TextPrimary,
        )
    }
    Slider(
        value = budgetHours,
        onValueChange = onBudgetHoursChanged,
        valueRange = ScreenTimeRepository.MIN_BUDGET_HOURS..ScreenTimeRepository.MAX_BUDGET_HOURS,
        steps = 22,
        colors = SliderDefaults.colors(
            thumbColor = KanColors.SteelHighlight,
            activeTrackColor = KanColors.Steel,
            inactiveTrackColor = KanColors.Hairline,
            activeTickColor = KanColors.Steel.copy(alpha = 0.42f),
            inactiveTickColor = KanColors.TextMuted,
        ),
        modifier = Modifier.padding(top = 18.dp),
    )
}
