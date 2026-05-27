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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.core.LockScreenVisualization
import com.kan.app.domain.toClockTime
import com.kan.app.domain.toHumanDuration
import com.kan.app.ui.components.GuardingRing
import com.kan.app.ui.theme.KanColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val DAY_SECONDS = 24L * 60L * 60L
private val ChallengePresets = listOf(15, 30, 60, 120)

@Composable
fun LockTimerScreen(
    currentAbsenceSeconds: Long,
    todayAwaySeconds: Long,
    visualization: LockScreenVisualization,
    challengeRemainingSeconds: Long,
    challengeDurationSeconds: Long,
    onStartChallenge: (minutes: Int) -> Unit,
    onCancelChallenge: () -> Unit,
) {
    val challengeActive = challengeDurationSeconds > 0L
    val challengeFinished = challengeActive && challengeRemainingSeconds <= 0L

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
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "I  IN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp,
                    color = KanColors.TextTertiary,
                )
                Spacer(Modifier.height(18.dp))

                if (challengeActive) {
                    ChallengePanel(
                        remainingSeconds = challengeRemainingSeconds,
                        durationSeconds = challengeDurationSeconds,
                        finished = challengeFinished,
                        onCancel = onCancelChallenge,
                    )
                } else {
                    AwayPanel(
                        currentAbsenceSeconds = currentAbsenceSeconds,
                        todayAwaySeconds = todayAwaySeconds,
                    )
                    Spacer(Modifier.height(20.dp))
                    ChallengeStarter(onStart = onStartChallenge)
                }

                Spacer(Modifier.height(22.dp))
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.4.sp,
                    color = KanColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun AwayPanel(currentAbsenceSeconds: Long, todayAwaySeconds: Long) {
    Text(
        text = "TIME SINCE YOU LOCKED",
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = KanColors.Steel,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = currentAbsenceSeconds.toClockTime(),
        fontSize = 58.sp,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-2.0).sp,
        color = KanColors.TextPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 60.sp,
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = "AWAY TODAY",
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
        color = KanColors.TextTertiary,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = (todayAwaySeconds + currentAbsenceSeconds).toHumanDuration(),
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.4).sp,
        color = KanColors.TextSecondary,
    )
}

@Composable
private fun ChallengePanel(
    remainingSeconds: Long,
    durationSeconds: Long,
    finished: Boolean,
    onCancel: () -> Unit,
) {
    Text(
        text = if (finished) "CHALLENGE COMPLETE" else "CHALLENGE IN PROGRESS",
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = if (finished) KanColors.PrismGreen else KanColors.Steel,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = if (finished) "00:00:00" else remainingSeconds.toClockTime(),
        fontSize = 58.sp,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-2.0).sp,
        color = KanColors.TextPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 60.sp,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = if (finished) {
            "You waited ${durationSeconds.toHumanDuration()}."
        } else {
            "Goal: ${durationSeconds.toHumanDuration()}"
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.4.sp,
        color = KanColors.TextSecondary,
    )
    Spacer(Modifier.height(14.dp))
    TextButton(onClick = onCancel) {
        Text(
            text = if (finished) "CLEAR" else "CANCEL CHALLENGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = KanColors.TextTertiary,
        )
    }
}

@Composable
private fun ChallengeStarter(onStart: (minutes: Int) -> Unit) {
    Text(
        text = "CHALLENGE YOURSELF",
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
        color = KanColors.TextTertiary,
    )
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 11.sp,
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
) {
    val progress = challengeProgress ?: (todayAwaySeconds.toFloat() / DAY_SECONDS.toFloat())
        .coerceIn(0f, 1f)

    when (visualization) {
        LockScreenVisualization.Arc -> ArcGauge(progress = progress)
        LockScreenVisualization.Pillar -> PillarGauge(progress = progress)
        LockScreenVisualization.Constellation -> ConstellationGauge(progress = progress)
    }
}

@Composable
private fun ArcGauge(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.40f
        val stroke = w * 0.018f

        val sweep = 260f
        val startAngle = 140f

        drawArc(
            color = KanColors.Hairline.copy(alpha = 0.7f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke),
        )

        if (progress > 0f) {
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
                sweepAngle = sweep * progress,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke * 1.4f),
            )
        }
    }
}

@Composable
private fun PillarGauge(progress: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
                .width(6.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(KanColors.Hairline.copy(alpha = 0.6f)),
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
                .width(6.dp)
                .height(280.dp),
        ) {
            val filledHeight = size.height * progress.coerceIn(0f, 1f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.00f to KanColors.PrismViolet.copy(alpha = 0.85f),
                    0.30f to KanColors.PrismBlue,
                    0.55f to KanColors.PrismGreen,
                    0.80f to KanColors.PrismGold,
                    1.00f to KanColors.PrismRed.copy(alpha = 0.85f),
                ),
                topLeft = Offset(0f, size.height - filledHeight),
                size = Size(size.width, filledHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f),
            )
        }
    }
}

@Composable
private fun ConstellationGauge(progress: Float) {
    val totalDots = 24
    val lit = (progress.coerceIn(0f, 1f) * totalDots).toInt()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) * 0.36f
        val dotRadius = w * 0.012f

        repeat(totalDots) { index ->
            val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * index / totalDots)
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()

            val isLit = index < lit
            drawCircle(
                color = if (isLit) {
                    prismHue(index, totalDots)
                } else {
                    KanColors.Hairline.copy(alpha = 0.7f)
                },
                radius = if (isLit) dotRadius * 1.4f else dotRadius,
                center = Offset(x, y),
            )
        }
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
