package com.kan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.core.LockScreenVisualization
import com.kan.app.domain.toClockTime
import com.kan.app.domain.toHumanDuration
import com.kan.app.ui.components.GuardingRing
import com.kan.app.ui.theme.KanColors
import com.kan.app.ui.theme.KanTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val DAY_SECONDS = 24L * 60L * 60L
private val ChallengePresets = listOf(15, 30, 60, 120)

@Composable
fun LockTimerScreen(
    currentAbsenceSeconds: Long,
    todayAwaySeconds: Long,
    dailyChallengeSuccesses: Int,
    visualization: LockScreenVisualization,
    challengeRemainingSeconds: Long,
    challengeDurationSeconds: Long,
    onStartChallenge: (minutes: Int) -> Unit,
    onCancelChallenge: () -> Unit,
) {
    val challengeActive = challengeDurationSeconds > 0L
    val challengeFinished = challengeActive && challengeRemainingSeconds <= 0L
    var showCancelConfirmation by remember { mutableStateOf(false) }

    fun requestChallengeCancel() {
        if (challengeActive && !challengeFinished) {
            showCancelConfirmation = true
        } else {
            onCancelChallenge()
        }
    }

    BackHandler(enabled = challengeActive) {
        requestChallengeCancel()
    }

    if (showCancelConfirmation) {
        ChallengeCancelDialog(
            onDismiss = { showCancelConfirmation = false },
            onConfirm = {
                showCancelConfirmation = false
                onCancelChallenge()
            },
        )
    }

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

            VisualizationLayer(
                visualization = visualization,
                todayAwaySeconds = todayAwaySeconds,
                challengeProgress = if (challengeActive && challengeDurationSeconds > 0L) {
                    1f - (challengeRemainingSeconds.toFloat() / challengeDurationSeconds.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    null
                },
                timerSeconds = if (challengeActive) {
                    challengeRemainingSeconds.coerceAtLeast(0L)
                } else {
                    currentAbsenceSeconds.coerceAtLeast(0L)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "I  IN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp,
                    color = KanColors.TextTertiary,
                )
                Spacer(Modifier.height(8.dp))

                if (challengeActive) {
                    ChallengePanel(
                        remainingSeconds = challengeRemainingSeconds,
                        durationSeconds = challengeDurationSeconds,
                        finished = challengeFinished,
                        dailyChallengeSuccesses = dailyChallengeSuccesses,
                        onCancel = ::requestChallengeCancel,
                    )
                } else {
                    AwayPanel(
                        currentAbsenceSeconds = currentAbsenceSeconds,
                        todayAwaySeconds = todayAwaySeconds,
                        dailyChallengeSuccesses = dailyChallengeSuccesses,
                    )
                    Spacer(Modifier.height(10.dp))
                    ChallengeStarter(onStart = onStartChallenge)
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (challengeActive) {
                        if (challengeFinished) {
                            "Challenge complete. Unlock when you’re ready."
                        } else {
                            "Keep the phone down. The countdown is your promise to yourself."
                        }
                    } else {
                        "Top number: time since you locked the screen.\n" +
                            "Below: total time away today, resets at midnight."
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.4.sp,
                    color = KanColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun AwayPanel(
    currentAbsenceSeconds: Long,
    todayAwaySeconds: Long,
    dailyChallengeSuccesses: Int,
) {
    Text(
        text = "TIME SINCE YOU LOCKED",
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = KanColors.Steel,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = currentAbsenceSeconds.toClockTime(),
        fontSize = 46.sp,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-2.0).sp,
        color = KanColors.TextPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 48.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "AWAY TODAY",
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
        color = KanColors.TextTertiary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = todayAwaySeconds.toHumanDuration(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.4).sp,
        color = KanColors.TextSecondary,
    )
    Spacer(Modifier.height(8.dp))
    DailyChallengeSuccesses(count = dailyChallengeSuccesses)
}

@Composable
private fun ChallengePanel(
    remainingSeconds: Long,
    durationSeconds: Long,
    finished: Boolean,
    dailyChallengeSuccesses: Int,
    onCancel: () -> Unit,
) {
    Text(
        text = if (finished) "CHALLENGE COMPLETE" else "CHALLENGE IN PROGRESS",
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = if (finished) KanColors.PrismGreen else KanColors.Steel,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = if (finished) "00:00:00" else remainingSeconds.toClockTime(),
        fontSize = 46.sp,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-2.0).sp,
        color = KanColors.TextPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 48.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = if (finished) {
            "You waited ${durationSeconds.toHumanDuration()}."
        } else {
            "Goal: ${durationSeconds.toHumanDuration()}"
        },
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.4.sp,
        color = KanColors.TextSecondary,
    )
    Spacer(Modifier.height(8.dp))
    DailyChallengeSuccesses(count = dailyChallengeSuccesses)
    Spacer(Modifier.height(10.dp))
    TextButton(onClick = onCancel) {
        Text(
            text = if (finished) "CLEAR" else "CANCEL CHALLENGE",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun DailyChallengeSuccesses(count: Int) {
    Text(
        text = "CHALLENGES WON TODAY",
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
        color = KanColors.TextTertiary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = count.toString(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.2).sp,
        color = if (count > 0) KanColors.PrismGreen else KanColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ChallengeCancelDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KanColors.VoidElevated,
        titleContentColor = KanColors.TextPrimary,
        textContentColor = KanColors.TextSecondary,
        title = {
            Text(
                text = "Cancel challenge?",
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Text("Leaving this screen now cancels the active challenge and it will not count as a win.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("CANCEL CHALLENGE", color = KanColors.PrismRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("KEEP GOING", color = KanColors.Steel)
            }
        },
    )
}

@Composable
private fun ChallengeStarter(onStart: (minutes: Int) -> Unit) {
    Text(
        text = "CHALLENGE YOURSELF",
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
        color = KanColors.TextTertiary,
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        ChallengePresets.forEach { minutes ->
            ChallengeChip(label = labelFor(minutes), onClick = { onStart(minutes) })
        }
    }
}

@Composable
private fun ChallengeChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = KanColors.VoidElevated,
        border = BorderStroke(0.6.dp, KanColors.SteelShadow.copy(alpha = 0.4f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = KanColors.TextPrimary,
        )
    }
}

private fun labelFor(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes}m"
}

@Composable
private fun VisualizationLayer(
    visualization: LockScreenVisualization,
    todayAwaySeconds: Long,
    challengeProgress: Float?,
    timerSeconds: Long,
) {
    val progress = challengeProgress ?: (todayAwaySeconds.toFloat() / DAY_SECONDS.toFloat())
        .coerceIn(0f, 1f)

    when (visualization) {
        LockScreenVisualization.Arc -> ArcGauge(progress = progress, timerSeconds = timerSeconds)
        LockScreenVisualization.Pillar -> PillarGauge(progress = progress, timerSeconds = timerSeconds)
        LockScreenVisualization.Constellation -> ConstellationGauge(progress = progress, timerSeconds = timerSeconds)
    }
}

@Composable
private fun ArcGauge(progress: Float, timerSeconds: Long) {
    val safeSeconds = timerSeconds.coerceAtLeast(0L)
    val minuteProgress = ((safeSeconds / 60L) % 60L) / 60f
    val secondProgress = (safeSeconds % 60L) / 60f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = w * 0.40f
        val outerStroke = w * 0.018f
        val middleStroke = w * 0.014f
        val innerStroke = w * 0.010f
        val bandGap = w * 0.020f

        val sweep = 260f
        val startAngle = 140f
        fun drawBand(radius: Float, stroke: Float, bandProgress: Float, alpha: Float) {
            drawArc(
                color = KanColors.Hairline.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke),
            )

            if (bandProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.00f to KanColors.PrismRed.copy(alpha = 0.55f),
                        0.20f to KanColors.PrismGold,
                        0.45f to KanColors.PrismGreen,
                        0.70f to KanColors.PrismBlue,
                        0.95f to KanColors.PrismViolet.copy(alpha = 0.75f),
                        1.00f to KanColors.PrismRed.copy(alpha = 0.55f),
                        center = Offset(cx, cy),
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep * bandProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = stroke * 1.2f),
                )
            }
        }

        drawBand(baseRadius, outerStroke, progress, 0.70f)
        drawBand(baseRadius - bandGap, middleStroke, minuteProgress, 0.58f)
        drawBand(baseRadius - bandGap * 2, innerStroke, secondProgress, 0.48f)
    }
}

@Composable
private fun PillarGauge(progress: Float, timerSeconds: Long) {
    val safeSeconds = timerSeconds.coerceAtLeast(0L)
    val secondProgress = (safeSeconds % 60L) / 60f
    val minuteProgress = ((safeSeconds / 60L) % 60L) / 60f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickBar(
            progress = progress.coerceIn(0f, 1f),
            width = 8.dp,
            alpha = 1f,
        )
        Spacer(Modifier.width(8.dp))
        TickBar(
            progress = minuteProgress,
            width = 6.dp,
            alpha = 0.8f,
        )
        Spacer(Modifier.width(6.dp))
        TickBar(
            progress = secondProgress,
            width = 4.dp,
            alpha = 0.65f,
        )
    }
}

@Composable
private fun TickBar(progress: Float, width: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .width(width)
            .height(280.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(KanColors.Hairline.copy(alpha = 0.45f * alpha)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val filledHeight = size.height * progress.coerceIn(0f, 1f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.00f to KanColors.PrismViolet.copy(alpha = 0.9f * alpha),
                    0.35f to KanColors.PrismBlue.copy(alpha = alpha),
                    0.60f to KanColors.PrismGreen.copy(alpha = alpha),
                    0.85f to KanColors.PrismGold.copy(alpha = 0.95f * alpha),
                    1.00f to KanColors.PrismRed.copy(alpha = 0.9f * alpha),
                ),
                topLeft = Offset(0f, size.height - filledHeight),
                size = Size(size.width, filledHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f),
            )
        }
    }
}

@Composable
private fun ConstellationGauge(progress: Float, timerSeconds: Long) {
    val safeSeconds = timerSeconds.coerceAtLeast(0L)
    val minuteProgress = ((safeSeconds / 60L) % 60L) / 60f
    val secondProgress = (safeSeconds % 60L) / 60f
    val totalDots = 24

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerRadius = min(w, h) * 0.36f
        val middleRadius = outerRadius - (w * 0.05f)
        val innerRadius = outerRadius - (w * 0.10f)
        val outerDotRadius = w * 0.012f
        val middleDotRadius = w * 0.010f
        val innerDotRadius = w * 0.008f

        fun drawRing(radius: Float, dotRadius: Float, litProgress: Float, dimAlpha: Float) {
            val lit = (litProgress.coerceIn(0f, 1f) * totalDots).toInt()
            repeat(totalDots) { index ->
                val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * index / totalDots)
                val x = cx + radius * cos(angle).toFloat()
                val y = cy + radius * sin(angle).toFloat()

                val isLit = index < lit
                drawCircle(
                    color = if (isLit) prismHue(index, totalDots) else KanColors.Hairline.copy(alpha = dimAlpha),
                    radius = if (isLit) dotRadius * 1.35f else dotRadius,
                    center = Offset(x, y),
                )
            }
        }

        drawRing(outerRadius, outerDotRadius, progress, 0.70f)
        drawRing(middleRadius, middleDotRadius, minuteProgress, 0.56f)
        drawRing(innerRadius, innerDotRadius, secondProgress, 0.44f)
    }
}

private fun prismHue(index: Int, total: Int): Color {
    val palette = listOf(
        KanColors.PrismRed,
        KanColors.PrismGold,
        KanColors.PrismGreen,
        KanColors.PrismBlue,
        KanColors.PrismViolet,
    )
    val slot = (index.toFloat() / total * palette.size).toInt().coerceIn(0, palette.lastIndex)
    return palette[slot]
}

@Preview(
    name = "Lock timer · away",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun LockTimerScreenAwayPreview() {
    KanTheme {
        LockTimerScreen(
            currentAbsenceSeconds = 42L * 60 + 18,
            todayAwaySeconds = 4L * 60 * 60 + 5 * 60,
            dailyChallengeSuccesses = 4,
            visualization = LockScreenVisualization.Arc,
            challengeRemainingSeconds = 0L,
            challengeDurationSeconds = 0L,
            onStartChallenge = {},
            onCancelChallenge = {},
        )
    }
}

@Preview(
    name = "Lock timer · active challenge",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    backgroundColor = 0xFF04060A,
)
@Composable
private fun LockTimerScreenChallengePreview() {
    KanTheme {
        LockTimerScreen(
            currentAbsenceSeconds = 12L * 60,
            todayAwaySeconds = 4L * 60 * 60 + 5 * 60,
            dailyChallengeSuccesses = 4,
            visualization = LockScreenVisualization.Constellation,
            challengeRemainingSeconds = 18L * 60,
            challengeDurationSeconds = 30L * 60,
            onStartChallenge = {},
            onCancelChallenge = {},
        )
    }
}
