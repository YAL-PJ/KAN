package com.kan.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kan.app.data.DailyHistoryEntry
import com.kan.app.data.KanSnapshot
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.domain.toClockTime
import com.kan.app.domain.toHumanDuration
import com.kan.app.service.ScreenTimeService
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var repository: ScreenTimeRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startTrackingService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScreenTimeRepository.get(this)
        requestNotificationPermissionIfNeeded()
        startTrackingService()

        setContent {
            val snapshot by repository.snapshots.collectAsStateWithLifecycle()
            KanTheme {
                KanApp(
                    snapshot = snapshot,
                    hasOverlayPermission = Settings.canDrawOverlays(this),
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onBudgetHoursChanged = repository::updateDailyBudgetHours,
                    onLockTimerModeChanged = repository::updateLockTimerMode,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startTrackingService()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun startTrackingService() {
        ScreenTimeService.start(this)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KanApp(
    snapshot: KanSnapshot,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onBudgetHoursChanged: (Float) -> Unit,
    onLockTimerModeChanged: (Int) -> Unit,
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
                    lockTimerMode = snapshot.lockTimerMode,
                    onLockTimerModeChanged = onLockTimerModeChanged,
                )
                1 -> HistorySettingsScreen(snapshot, onBudgetHoursChanged)
            }
        }
    }
}

@Composable
private fun MainHubScreen(
    snapshot: KanSnapshot,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    lockTimerMode: Int,
    onLockTimerModeChanged: (Int) -> Unit,
) {
    KanScaffold {
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
        SectionLabel("DEV: LOCK SCREEN TIMER MODE")
        Spacer(Modifier.height(10.dp))
        LockTimerModeOption(
            title = "Option A: Passive chronometer",
            subtitle = "Standard low-priority ongoing notification timer.",
            selected = lockTimerMode == ScreenTimeRepository.LOCK_TIMER_MODE_CHRONOMETER,
            onSelect = { onLockTimerModeChanged(ScreenTimeRepository.LOCK_TIMER_MODE_CHRONOMETER) },
        )
        LockTimerModeOption(
            title = "Option B: Full-screen takeover",
            subtitle = "Launches a large lock-screen timer activity.",
            selected = lockTimerMode == ScreenTimeRepository.LOCK_TIMER_MODE_FULL_SCREEN,
            onSelect = { onLockTimerModeChanged(ScreenTimeRepository.LOCK_TIMER_MODE_FULL_SCREEN) },
        )
        LockTimerModeOption(
            title = "Option C: Heads-up banner",
            subtitle = "Triggers a high-priority banner with live absence value.",
            selected = lockTimerMode == ScreenTimeRepository.LOCK_TIMER_MODE_BANNER,
            onSelect = { onLockTimerModeChanged(ScreenTimeRepository.LOCK_TIMER_MODE_BANNER) },
        )

        if (!hasOverlayPermission) {
            Spacer(Modifier.height(40.dp))
            TextButton(
                onClick = onRequestOverlayPermission,
                colors = TextButtonDefaults.textButtonColors(contentColor = KanColors.Accent),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Grant floating timer access",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
            }
        }

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
private fun HistorySettingsScreen(
    snapshot: KanSnapshot,
    onBudgetHoursChanged: (Float) -> Unit,
) {
    val budgetHours = snapshot.dailyBudgetSeconds / 3_600f
    KanScaffold {
        Text(
            text = "HISTORY",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
            color = KanColors.TextPrimary,
        )
        Spacer(Modifier.height(34.dp))
        if (snapshot.history.isEmpty()) {
            Text(
                text = "A completed day will appear here.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                color = KanColors.TextSecondary,
                lineHeight = 24.sp,
            )
        } else {
            snapshot.history.forEach { entry ->
                HistoryRow(entry)
            }
        }

        Spacer(Modifier.height(54.dp))
        Hairline()
        Spacer(Modifier.height(54.dp))

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
            valueRange = 0.5f..12f,
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
}

@Composable
private fun HistoryRow(entry: DailyHistoryEntry) {
    val label = entry.date.format(DateTimeFormatter.ofPattern("EEE"))
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
private fun KanScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KanColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp, vertical = 56.dp),
            content = content,
        )
    }
}

@Composable
private fun CeremonialMetric(
    label: String,
    primary: String,
    secondary: String,
    support: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(label)
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = (-1.2).sp,
                color = KanColors.TextPrimary,
                lineHeight = 44.sp,
            )
            Text(
                text = "  $secondary",
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.2.sp,
                color = KanColors.TextTertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = support.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.7.sp,
            color = KanColors.TextSecondary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
        color = KanColors.TextTertiary,
    )
}

@Composable
private fun Hairline(alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KanColors.Hairline.copy(alpha = alpha)),
    )
}

private object KanColors {
    val Background = Color(0xFF080A0D)
    val Surface = Color(0xFF0D1014)
    val TextPrimary = Color(0xFFE7E1D5)
    val TextSecondary = Color(0xFFB7AFA2)
    val TextTertiary = Color(0xFF7E766B)
    val TextMuted = Color(0xFF565047)
    val Hairline = Color(0xFF2B3037)
    val Accent = Color(0xFFC2A66B)
}

@Composable
private fun KanTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = KanColors.Background,
        surface = KanColors.Surface,
        onBackground = KanColors.TextPrimary,
        onSurface = KanColors.TextPrimary,
        primary = KanColors.Accent,
        onPrimary = KanColors.Background,
        secondary = KanColors.TextSecondary,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
        ),
        content = content,
    )
}
