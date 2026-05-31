package com.kan.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.kan.app.ui.theme.KanTheme
import com.kan.app.ui.preview.PreviewFixtures
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

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
            HistoryExplorer(snapshot)
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


private enum class QuickHistoryWindow(val label: String, val days: Int) {
    Day("1 DAY", 1),
    Week("7 DAYS", 7),
}

private data class DailyChartPoint(
    val date: LocalDate,
    val phoneSeconds: Long,
    val awaySeconds: Long,
    val challengeAttempts: Int,
    val challengeSuccesses: Int,
    val metBudget: Boolean,
)

@Composable
private fun HistoryExplorer(snapshot: KanSnapshot) {
    var visibleDays by remember(snapshot.today, snapshot.history.size) {
        mutableIntStateOf(7.coerceAtMost(maxHistoryDays(snapshot)).coerceAtLeast(1))
    }
    val hasSavedHistory = snapshot.history.isNotEmpty() || snapshot.phoneSessions.isNotEmpty() ||
        snapshot.challengeAttempts.isNotEmpty()
    val maxDays = maxHistoryDays(snapshot)
    val points = remember(snapshot, visibleDays) { chartPoints(snapshot, visibleDays) }

    SectionLabel("HISTORY CHART")
    Spacer(Modifier.height(16.dp))
    QuickWindowToggle(
        selectedDays = visibleDays,
        maxDays = maxDays,
        onSelectedDays = { visibleDays = it },
    )
    Spacer(Modifier.height(16.dp))
    UsageHistoryChart(points = points, budgetSeconds = snapshot.dailyBudgetSeconds)
    Spacer(Modifier.height(14.dp))
    ChartLegend()
    Spacer(Modifier.height(16.dp))
    ZoomControls(
        visibleDays = visibleDays,
        maxDays = maxDays,
        hasSavedHistory = hasSavedHistory,
        onVisibleDaysChanged = { visibleDays = it.coerceIn(1, maxDays) },
    )
    Hairline(alpha = 0.5f)
    TodayChallengeWins(snapshot.dailyChallengeSuccesses)
    if (snapshot.history.isNotEmpty()) {
        Hairline(alpha = 0.5f)
    }
    HistoryList(snapshot.history.take(7))
    RecentChallengeAttempts(snapshot.challengeAttempts)
}

@Composable
private fun QuickWindowToggle(
    selectedDays: Int,
    maxDays: Int,
    onSelectedDays: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickHistoryWindow.entries.forEach { window ->
            val days = window.days.coerceAtMost(maxDays).coerceAtLeast(1)
            val selected = selectedDays == days
            TextButton(
                onClick = { onSelectedDays(days) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) KanColors.Steel.copy(alpha = 0.3f) else Color.Transparent,
                    contentColor = if (selected) KanColors.TextPrimary else KanColors.TextSecondary,
                ),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = window.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
            }
        }
    }
}

@Composable
private fun UsageHistoryChart(
    points: List<DailyChartPoint>,
    budgetSeconds: Long,
) {
    val maxSeconds = max(
        budgetSeconds,
        points.maxOfOrNull { max(it.phoneSeconds, it.awaySeconds) } ?: 0L,
    ).coerceAtLeast(60L)
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        ) {
            val chartTop = 10f
            val chartBottom = size.height - 30f
            val chartHeight = chartBottom - chartTop
            val widthPerDay = size.width / points.size.coerceAtLeast(1)
            val barWidth = (widthPerDay * 0.26f).coerceIn(3f, 18f)
            val budgetY = chartBottom - (budgetSeconds.toFloat() / maxSeconds.toFloat()) * chartHeight

            repeat(4) { index ->
                val y = chartTop + (chartHeight / 3f) * index
                drawLine(
                    color = KanColors.Hairline.copy(alpha = if (index == 3) 0.5f else 0.22f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            drawLine(
                color = KanColors.PrismGreen.copy(alpha = 0.58f),
                start = Offset(0f, budgetY),
                end = Offset(size.width, budgetY),
                strokeWidth = 2f,
            )

            points.forEachIndexed { index, point ->
                val centerX = widthPerDay * index + widthPerDay / 2f
                val phoneHeight = (point.phoneSeconds.toFloat() / maxSeconds.toFloat()) * chartHeight
                val awayHeight = (point.awaySeconds.toFloat() / maxSeconds.toFloat()) * chartHeight
                val attemptsHeight = if (point.challengeAttempts > 0) {
                    (8f + point.challengeAttempts * 4f).coerceAtMost(chartHeight * 0.36f)
                } else {
                    0f
                }

                drawRoundRect(
                    color = KanColors.Steel.copy(alpha = if (point.phoneSeconds > 0) 0.95f else 0.18f),
                    topLeft = Offset(centerX - barWidth - 2f, chartBottom - phoneHeight),
                    size = Size(barWidth, phoneHeight.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                )
                drawRoundRect(
                    color = KanColors.PrismGreen.copy(alpha = if (point.awaySeconds > 0) 0.85f else 0.14f),
                    topLeft = Offset(centerX + 2f, chartBottom - awayHeight),
                    size = Size(barWidth, awayHeight.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                )
                if (attemptsHeight > 0f) {
                    drawCircle(
                        color = if (point.challengeSuccesses > 0) KanColors.PrismGreen else KanColors.TextMuted,
                        radius = 4.5f,
                        center = Offset(centerX, chartBottom - max(phoneHeight, awayHeight) - attemptsHeight),
                    )
                }
                if (point.metBudget && point.phoneSeconds > 0L) {
                    drawCircle(
                        color = KanColors.PrismGreen.copy(alpha = 0.55f),
                        radius = (widthPerDay * 0.22f).coerceIn(7f, 15f),
                        center = Offset(centerX, chartBottom + 14f),
                        style = Stroke(width = 1.5f),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.firstOrNull()?.date?.format(WeekdayFormatter)?.uppercase().orEmpty(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = KanColors.TextTertiary,
            )
            Text(
                text = "BUDGET ${budgetSeconds.toHumanDuration()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = KanColors.PrismGreen,
            )
            Text(
                text = points.lastOrNull()?.date?.format(WeekdayFormatter)?.uppercase().orEmpty(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = KanColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun ChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(KanColors.Steel, "PHONE")
        LegendItem(KanColors.PrismGreen, "AWAY")
        LegendItem(KanColors.TextMuted, "CHALLENGES")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color.copy(alpha = 0.8f), RoundedCornerShape(999.dp)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun ZoomControls(
    visibleDays: Int,
    maxDays: Int,
    hasSavedHistory: Boolean,
    onVisibleDaysChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ZOOM",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextTertiary,
        )
        Text(
            text = "$visibleDays DAY${if (visibleDays == 1) "" else "S"}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = KanColors.TextSecondary,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onVisibleDaysChanged(visibleDays - 1) }, enabled = visibleDays > 1) {
            Text("−", fontSize = 18.sp)
        }
        Slider(
            value = visibleDays.toFloat(),
            onValueChange = { onVisibleDaysChanged(it.toInt().coerceAtLeast(1)) },
            valueRange = 1f..maxDays.toFloat(),
            steps = (maxDays - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = KanColors.SteelHighlight,
                activeTrackColor = KanColors.Steel,
                inactiveTrackColor = KanColors.Hairline,
            ),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onVisibleDaysChanged(visibleDays + 1) }, enabled = visibleDays < maxDays) {
            Text("+", fontSize = 18.sp)
        }
    }
    Text(
        text = if (!hasSavedHistory) {
            "No older data yet · empty range is capped at 30 days."
        } else {
            "Zoom out to every saved day; empty days stay visible inside the range."
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.Light,
        color = KanColors.TextMuted,
        lineHeight = 18.sp,
    )
}

@Composable
private fun RecentChallengeAttempts(attempts: List<com.kan.app.data.ChallengeAttemptEntry>) {
    if (attempts.isEmpty()) return
    Hairline(alpha = 0.5f)
    SectionLabel("LOCK-SCREEN CHALLENGE LOG")
    Spacer(Modifier.height(10.dp))
    attempts.take(3).forEach { attempt ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (attempt.successful) "SUCCESS" else "STOPPED",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = if (attempt.successful) KanColors.PrismGreen else KanColors.TextTertiary,
            )
            Text(
                text = "${attempt.elapsedSeconds.toHumanDuration()} / ${attempt.plannedDurationSeconds.toHumanDuration()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = KanColors.TextSecondary,
            )
        }
    }
}

private fun maxHistoryDays(snapshot: KanSnapshot): Int {
    val oldestTrackedDate = listOfNotNull(
        snapshot.history.minByOrNull { it.date }?.date,
        snapshot.phoneSessions.minByOrNull { it.startedAtMillis }?.localDate(),
        snapshot.challengeAttempts.minByOrNull { it.startedAtMillis }?.localDate(),
    ).minOrNull()
    if (oldestTrackedDate == null) return 30
    return (java.time.temporal.ChronoUnit.DAYS.between(oldestTrackedDate, snapshot.today) + 1L)
        .coerceAtLeast(1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}


private fun chartPoints(snapshot: KanSnapshot, visibleDays: Int): List<DailyChartPoint> {
    val historyByDate = snapshot.history.associateBy { it.date }
    val phoneSessionsByDate = snapshot.phoneSessions.groupBy { it.localDate() }
    val attemptsByDate = snapshot.challengeAttempts.groupBy { it.localDate() }
    val firstDate = snapshot.today.minusDays((visibleDays - 1).toLong())
    return (0 until visibleDays).map { offset ->
        val date = firstDate.plusDays(offset.toLong())
        val history = historyByDate[date]
        val attempts = attemptsByDate[date].orEmpty()
        DailyChartPoint(
            date = date,
            phoneSeconds = when {
                date == snapshot.today -> snapshot.dailyScreenSeconds
                history != null -> history.screenSeconds
                else -> phoneSessionsByDate[date].orEmpty().sumOf { it.durationSeconds }
            },
            awaySeconds = when {
                date == snapshot.today -> snapshot.dailyAbsenceSeconds
                history != null -> history.absenceSeconds
                else -> 0L
            },
            challengeAttempts = attempts.size,
            challengeSuccesses = attempts.count { it.successful } + if (date == snapshot.today) snapshot.dailyChallengeSuccesses else 0,
            metBudget = history?.metBudget ?: (date == snapshot.today && snapshot.dailyScreenSeconds <= snapshot.dailyBudgetSeconds),
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

@Preview(
    name = "History settings · phone",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun HistorySettingsScreenPreview() {
    KanTheme {
        HistorySettingsScreen(
            snapshot = PreviewFixtures.completedSnapshot,
            onBudgetHoursChanged = {},
            onOverlayEnabledChanged = {},
            onLockScreenTimerEnabledChanged = {},
        )
    }
}

@Preview(
    name = "History settings · empty ledger",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun HistorySettingsScreenEmptyPreview() {
    KanTheme {
        HistorySettingsScreen(
            snapshot = PreviewFixtures.completedSnapshot.copy(
                history = emptyList(),
                dailyChallengeSuccesses = 0,
                overlayEnabled = false,
                lockScreenTimerEnabled = false,
            ),
            onBudgetHoursChanged = {},
            onOverlayEnabledChanged = {},
            onLockScreenTimerEnabledChanged = {},
        )
    }
}
