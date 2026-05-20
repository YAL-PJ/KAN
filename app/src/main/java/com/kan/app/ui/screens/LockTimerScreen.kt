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
import com.kan.app.ui.components.GuardingRing
import com.kan.app.ui.components.PrismaticBeam
import com.kan.app.ui.theme.KanColors

@Composable
fun LockTimerScreen(elapsedSeconds: Long) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KanColors.Void,
        contentColor = KanColors.TextPrimary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KanColors.Void),
        ) {
            GuardingRing(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "I  IN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp,
                    color = KanColors.TextTertiary,
                )
                Spacer(Modifier.height(20.dp))
                PrismaticBeam(heightDp = 28, widthDp = 2)
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "ABSENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = KanColors.Steel,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = elapsedSeconds.toClockTime(),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2.0).sp,
                    color = KanColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 64.sp,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Phone locked. The circle holds the line.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.4.sp,
                    color = KanColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
