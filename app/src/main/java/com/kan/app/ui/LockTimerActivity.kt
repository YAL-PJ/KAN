package com.kan.app.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kan.app.data.ScreenTimeRepository
import com.kan.app.ui.screens.LockTimerScreen
import com.kan.app.ui.theme.KanTheme
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
            val challengeActive = snapshot.challengeDurationSeconds > 0L

            // Tick: re-read the dual-clock derived values once per second.
            var currentAbsenceSeconds by remember { mutableLongStateOf(currentAbsenceSecondsNow()) }
            var challengeRemainingSeconds by remember {
                mutableLongStateOf(currentChallengeRemainingSecondsNow())
            }
            var todayAwaySeconds by remember { mutableLongStateOf(currentTodayAwaySecondsNow()) }

            LaunchedEffect(startedAtMillis, challengeActive) {
                while (startedAtMillis > 0L || challengeActive) {
                    currentAbsenceSeconds = currentAbsenceSecondsNow()
                    challengeRemainingSeconds = currentChallengeRemainingSecondsNow()
                    todayAwaySeconds = currentTodayAwaySecondsNow()
                    delay(1_000L)
                }
            }

            LaunchedEffect(startedAtMillis) {
                if (startedAtMillis <= 0L) finish()
            }

            KanTheme {
                LockTimerScreen(
                    currentAbsenceSeconds = currentAbsenceSeconds,
                    todayAwaySeconds = todayAwaySeconds,
                    visualization = snapshot.lockScreenVisualization,
                    challengeRemainingSeconds = challengeRemainingSeconds,
                    challengeDurationSeconds = snapshot.challengeDurationSeconds,
                    onStartChallenge = { minutes ->
                        repository.startChallenge(minutes * 60L)
                    },
                    onCancelChallenge = repository::cancelChallenge,
                )
            }
        }
    }

    private fun currentAbsenceSecondsNow(): Long =
        (repository.currentAbsenceElapsedMillis() / 1_000L).coerceAtLeast(0L)

    private fun currentChallengeRemainingSecondsNow(): Long =
        (repository.currentChallengeRemainingMillis() / 1_000L).coerceAtLeast(0L)

    private fun currentTodayAwaySecondsNow(): Long =
        repository.currentDailyAbsenceSeconds(
            nowMillis = System.currentTimeMillis(),
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        )

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
        fun createIntent(context: Context): Intent =
            Intent(context, LockTimerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
