package com.kan.app.ui

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

            KanTheme {
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
        fun createIntent(context: Context): Intent =
            Intent(context, LockTimerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
