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
import com.kan.app.ui.components.Hairline
import com.kan.app.ui.components.KanScaffold
import com.kan.app.ui.components.SectionLabel
import com.kan.app.ui.theme.KanColors
import java.time.format.DateTimeFormatter

private const val SECONDS_PER_HOUR = 3_600f
private val WeekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

@Composable
fun HistorySettingsScreen(
    snapshot: KanSnapshot,
    onBudgetHoursChanged: (Float) -> Unit,
) {
    val budgetHours = snapshot.dailyBudgetSeconds / SECONDS_PER_HOUR
    KanScaffold {
        Text(
            text = "HISTORY",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
            color = KanColors.TextPrimary,
        )
        Spacer(Modifier.height(34.dp))
        HistoryList(snapshot.history)

        Spacer(Modifier.height(54.dp))
        Hairline()
        Spacer(Modifier.height(54.dp))

        BudgetSlider(
            budgetHours = budgetHours,
            onBudgetHoursChanged = onBudgetHoursChanged,
        )
    }
}

@Composable
private fun HistoryList(entries: List<DailyHistoryEntry>) {
    if (entries.isEmpty()) {
        Text(
            text = "A completed day will appear here.",
            fontSize = 17.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
            lineHeight = 24.sp,
        )
        return
    }
    entries.forEach { HistoryRow(it) }
}

@Composable
private fun HistoryRow(entry: DailyHistoryEntry) {
    val label = entry.date.format(WeekdayFormatter)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                color = KanColors.TextTertiary,
            )
            Text(
                text = "${entry.screenSeconds.toHumanDuration()} SCREEN  /  ${entry.peakAbsenceSeconds.toHumanDuration()} PEAK${if (entry.metBudget) "  ✓" else ""}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.End,
                color = if (entry.metBudget) KanColors.TextPrimary else KanColors.TextSecondary,
            )
        }
        Hairline(alpha = 0.08f)
    }
}

@Composable
private fun BudgetSlider(
    budgetHours: Float,
    onBudgetHoursChanged: (Float) -> Unit,
) {
    SectionLabel("DAILY SCREEN BUDGET")
    Spacer(Modifier.height(22.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Limit",
            fontSize = 18.sp,
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
            thumbColor = KanColors.Accent,
            activeTrackColor = KanColors.Accent,
            inactiveTrackColor = KanColors.Hairline,
            activeTickColor = KanColors.Accent.copy(alpha = 0.42f),
            inactiveTickColor = KanColors.TextMuted,
        ),
        modifier = Modifier.padding(top = 28.dp),
    )
}
