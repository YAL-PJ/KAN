package com.kan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.domain.toClockTime
import com.kan.app.ui.theme.KanColors

@Composable
fun LockTimerScreen(elapsedSeconds: Long) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KanColors.Background,
        contentColor = KanColors.TextPrimary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KanColors.Background)
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
                    color = KanColors.TextTertiary,
                )
                Spacer(Modifier.height(34.dp))
                Text(
                    text = "ABSENCE TIME",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.4.sp,
                    color = KanColors.Accent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = elapsedSeconds.toClockTime(),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-1.8).sp,
                    color = KanColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Phone locked. Your screen-time timer is paused.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.2.sp,
                    color = KanColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}
