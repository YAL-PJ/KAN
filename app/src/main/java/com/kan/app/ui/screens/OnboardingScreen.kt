package com.kan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.ui.components.FloatingPanel
import com.kan.app.ui.components.GuardingScaffold
import com.kan.app.ui.components.SectionLabel
import com.kan.app.ui.theme.KanColors

@Composable
fun OnboardingScreen(
    appName: String,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }

    GuardingScaffold {
        SectionLabel("STEP ${step + 1} OF 4")
        Spacer(Modifier.height(24.dp))

        when (step) {
            0 -> IntroStep(
                appName = appName,
                onContinue = { step = 1 },
            )
            1 -> TrackingStep(
                appName = appName,
                onContinue = { step = 2 },
            )
            2 -> NotificationStep(
                appName = appName,
                onAllow = {
                    onRequestNotificationPermission()
                    step = 3
                },
                onSkip = { step = 3 },
            )
            3 -> OverlayStep(
                onEnable = {
                    onRequestOverlayPermission()
                    onFinish()
                },
                onSkip = onFinish,
            )
        }
    }
}

@Composable
private fun IntroStep(appName: String, onContinue: () -> Unit) {
    FloatingPanel {
        OnboardingHeadline(appName)
        Spacer(Modifier.height(20.dp))
        OnboardingBody("$appName helps you spend less time on your phone.")
        Spacer(Modifier.height(8.dp))
        OnboardingBody("Put it down. Let the time away grow.")
        Spacer(Modifier.height(28.dp))
        OnboardingButton(text = "BEGIN", onClick = onContinue)
    }
}

@Composable
private fun TrackingStep(appName: String, onContinue: () -> Unit) {
    FloatingPanel {
        SectionLabel("WHAT IT TRACKS")
        Spacer(Modifier.height(18.dp))
        OnboardingBody("$appName tracks when your phone is active and when you stay away from it.")
        Spacer(Modifier.height(12.dp))
        OnboardingBody("No app-by-app statistics. No noise. Just two numbers: time on, time away.")
        Spacer(Modifier.height(28.dp))
        OnboardingButton(text = "CONTINUE", onClick = onContinue)
    }
}

@Composable
private fun NotificationStep(appName: String, onAllow: () -> Unit, onSkip: () -> Unit) {
    FloatingPanel {
        SectionLabel("NOTIFICATION ACCESS")
        Spacer(Modifier.height(18.dp))
        OnboardingBody("$appName uses a quiet foreground notification so tracking keeps running while the phone is locked.")
        Spacer(Modifier.height(28.dp))
        OnboardingButton(text = "ALLOW NOTIFICATIONS", onClick = onAllow)
        OnboardingButton(text = "NOT NOW", onClick = onSkip, muted = true)
    }
}

@Composable
private fun OverlayStep(onEnable: () -> Unit, onSkip: () -> Unit) {
    FloatingPanel {
        SectionLabel("OPTIONAL · FLOATING PILL")
        Spacer(Modifier.height(18.dp))
        OnboardingBody("Show a small floating pill with your current phone time. You can change this later.")
        Spacer(Modifier.height(28.dp))
        OnboardingButton(text = "ENABLE FLOATING PILL", onClick = onEnable)
        OnboardingButton(text = "SKIP FOR NOW", onClick = onSkip, muted = true)
    }
}

@Composable
private fun OnboardingHeadline(appName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = appName.uppercase(),
            fontSize = 44.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 6.sp,
            color = KanColors.TextPrimary,
            lineHeight = 48.sp,
        )
        Text(
            text = "GUARDING CIRCLE",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.6.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun OnboardingBody(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Light,
        color = KanColors.TextSecondary,
        lineHeight = 22.sp,
    )
}

@Composable
private fun OnboardingButton(text: String, onClick: () -> Unit, muted: Boolean = false) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = if (muted) KanColors.TextMuted else KanColors.Steel,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
