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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Surface(modifier = Modifier.fillMaxSize()) {
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
) {
    KanScaffold {
        Text(
            text = "KAN.",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(64.dp))
        SectionLabel("DAILY BUDGET")
        Text(
            text = "${snapshot.dailyScreenSeconds.toClockTime()} / ${snapshot.dailyBudgetSeconds.toClockTime()}",
            fontSize = 31.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.8).sp,
        )
        Text(
            text = "${snapshot.dailyBudgetStreak}-Day Budget Streak",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(52.dp))
        Hairline()
        Spacer(Modifier.height(44.dp))
        SectionLabel("CONTINUOUS ABSENCE")
        Text(
            text = "All-Time Record: ${snapshot.allTimeAbsenceRecordSeconds.toHumanDuration()}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.4).sp,
        )
        Text(
            text = "Last Session: ${snapshot.lastAbsenceSeconds.toHumanDuration()}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (!hasOverlayPermission) {
            Spacer(Modifier.height(36.dp))
            TextButton(onClick = onRequestOverlayPermission) {
                Text("Enable floating pill")
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "Swipe for history →",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
        )
    }
}

@Composable
private fun HistorySettingsScreen(
    snapshot: KanSnapshot,
    onBudgetHoursChanged: (Float) -> Unit,
) {
    val budgetHours = snapshot.dailyBudgetSeconds / 3_600f
    KanScaffold {
        SectionLabel("HISTORY")
        Spacer(Modifier.height(20.dp))
        if (snapshot.history.isEmpty()) {
            Text(
                text = "Your first completed day will appear here.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
            )
        } else {
            snapshot.history.forEach { entry ->
                HistoryRow(entry)
            }
        }
        Spacer(Modifier.height(44.dp))
        Hairline()
        Spacer(Modifier.height(44.dp))
        SectionLabel("SETTINGS")
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Daily Screen Budget", fontSize = 18.sp, fontWeight = FontWeight.Light)
            Text("%.1f Hrs".format(budgetHours), fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = budgetHours,
            onValueChange = onBudgetHoursChanged,
            valueRange = 0.5f..12f,
            steps = 22,
            modifier = Modifier.padding(top = 22.dp),
        )
    }
}

@Composable
private fun HistoryRow(entry: DailyHistoryEntry) {
    val label = entry.date.format(DateTimeFormatter.ofPattern("EEE"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(
            text = "${entry.screenSeconds.toHumanDuration()} Screen | Peak ${entry.peakAbsenceSeconds.toHumanDuration()}${if (entry.metBudget) " ✓" else ""}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun KanScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
    )
}

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
    )
}

@Composable
private fun KanTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            primary = Color.White,
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF7F1E8),
            surface = Color(0xFFF7F1E8),
            onBackground = Color(0xFF111111),
            onSurface = Color(0xFF111111),
            primary = Color(0xFF111111),
        )
    }
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
