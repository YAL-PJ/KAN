package com.kan.app

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.domain.toClockTime
import kotlinx.coroutines.delay

class LockTimerActivity : ComponentActivity() {
    private lateinit var repository: ScreenTimeRepository
    private var receiverRegistered = false

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT -> finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ScreenTimeRepository.get(this)
        showOnLockScreen()
        registerFinishReceiver()

        setContent {
            val snapshot by repository.snapshots.collectAsStateWithLifecycle()
            val startedAtMillis = snapshot.currentAbsenceStartedAtMillis
            var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

            LaunchedEffect(startedAtMillis) {
                while (startedAtMillis > 0L) {
                    nowMillis = System.currentTimeMillis()
                    delay(1_000L)
                }
            }

            LaunchedEffect(startedAtMillis) {
                if (startedAtMillis <= 0L) finish()
            }

            LockTimerTheme {
                LockTimerScreen(
                    elapsedSeconds = if (startedAtMillis > 0L) {
                        ((nowMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)
                    } else {
                        0L
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isKeyguardLocked()) finish()
    }

    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(finishReceiver)
        super.onDestroy()
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }

    private fun registerFinishReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun isKeyguardLocked(): Boolean =
        getSystemService(KeyguardManager::class.java).isKeyguardLocked

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, LockTimerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

@Composable
private fun LockTimerScreen(elapsedSeconds: Long) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LockTimerColors.Background,
        contentColor = LockTimerColors.TextPrimary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LockTimerColors.Background)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "KAN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.2.sp,
                    color = LockTimerColors.TextTertiary,
                )
                Spacer(Modifier.height(34.dp))
                Text(
                    text = "ABSENCE TIME",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.4.sp,
                    color = LockTimerColors.Accent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = elapsedSeconds.toClockTime(),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-1.8).sp,
                    color = LockTimerColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Phone locked. Your screen-time timer is paused.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.2.sp,
                    color = LockTimerColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun LockTimerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = LockTimerColors.Background,
        surface = LockTimerColors.Background,
        onBackground = LockTimerColors.TextPrimary,
        onSurface = LockTimerColors.TextPrimary,
        primary = LockTimerColors.Accent,
        onPrimary = LockTimerColors.Background,
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

private object LockTimerColors {
    val Background = Color(0xFF080A0D)
    val TextPrimary = Color(0xFFE7E1D5)
    val TextSecondary = Color(0xFFB7AFA2)
    val TextTertiary = Color(0xFF7E766B)
    val Accent = Color(0xFFC2A66B)
}
