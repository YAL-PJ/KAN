package com.kan.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.ui.theme.KanColors

/**
 * Full-screen container that frames everything inside a brushed-steel "guarding circle".
 * The ring is drawn behind the content with depth shadows and a faint outer orbit arc;
 * content floats above, scrolls vertically, and never crosses the ring's optical guard rails.
 */
@Composable
fun GuardingScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KanColors.Void),
    ) {
        GuardingRing(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 38.dp, vertical = 64.dp),
            content = content,
        )
    }
}

/**
 * Draws the brushed-steel guarding circle that wraps the screen, plus a faint outer
 * orbit and a soft inner shadow. The ring is slightly wider than the screen so the
 * top and bottom curves fall off-edge — the viewer reads it as the screen sitting
 * inside the protective circle, with the left and right curves visible as guard rails.
 */
@Composable
fun GuardingRing(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.56f
        val stroke = w * 0.022f

        // Faint outer orbit — depth, "atmosphere" beyond the guard
        drawCircle(
            color = KanColors.Hairline.copy(alpha = 0.55f),
            radius = radius + stroke * 3.2f,
            center = Offset(cx, cy),
            style = Stroke(width = 0.6.dp.toPx()),
        )
        drawCircle(
            color = KanColors.Hairline.copy(alpha = 0.18f),
            radius = radius + stroke * 5.4f,
            center = Offset(cx + w * 0.18f, cy + h * 0.04f),
            style = Stroke(width = 0.5.dp.toPx()),
        )

        // Soft drop shadow under the ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy + stroke * 0.8f),
                radius = radius + stroke * 1.6f,
            ),
            radius = radius + stroke * 1.6f,
            center = Offset(cx, cy + stroke * 0.8f),
            style = Stroke(width = stroke * 2.2f),
        )

        // Main brushed-steel ring — sweep gradient lit from upper-left
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    KanColors.Steel,
                    KanColors.SteelShadow,
                    KanColors.SteelDeep,
                    KanColors.SteelShadow,
                    KanColors.Steel,
                    KanColors.SteelHighlight,
                    KanColors.SteelHighlight,
                    KanColors.Steel,
                    KanColors.Steel,
                ),
                center = Offset(cx, cy),
            ),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )

        // Inner edge highlight — gives the metal its bevel
        drawCircle(
            color = KanColors.SteelHighlight.copy(alpha = 0.32f),
            radius = radius - stroke * 0.5f,
            center = Offset(cx, cy),
            style = Stroke(width = 0.5.dp.toPx()),
        )

        // Inner core fade — pools depth toward the focal area
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    KanColors.VoidGlass.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 0.85f,
            ),
            radius = radius * 0.85f,
            center = Offset(cx, cy),
        )
    }
}

/**
 * Thin prismatic light bar — the focal accent borrowed from the icon. Use sparingly,
 * as a single moment of color inside an otherwise monochrome composition.
 */
@Composable
fun PrismaticBeam(
    modifier: Modifier = Modifier,
    heightDp: Int = 96,
    widthDp: Int = 4,
) {
    Box(
        modifier = modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(widthDp.dp))
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.10f to KanColors.PrismRed.copy(alpha = 0.55f),
                    0.30f to KanColors.PrismGold,
                    0.50f to KanColors.PrismGreen.copy(alpha = 0.85f),
                    0.72f to KanColors.PrismBlue.copy(alpha = 0.75f),
                    0.90f to KanColors.PrismViolet.copy(alpha = 0.55f),
                    1.00f to Color.Transparent,
                ),
            ),
    )
}

/**
 * Floating glass panel — content sits above the void with soft elevation and a
 * brushed-steel hairline border that catches the ring's light.
 */
@Composable
fun FloatingPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KanColors.VoidElevated,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 20.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(
            width = 0.6.dp,
            brush = Brush.verticalGradient(
                listOf(
                    KanColors.SteelHighlight.copy(alpha = 0.28f),
                    KanColors.SteelShadow.copy(alpha = 0.18f),
                    KanColors.SteelDeep.copy(alpha = 0.10f),
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String, color: Color = KanColors.TextTertiary) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
        color = color,
    )
}

@Composable
fun Hairline(alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        KanColors.Hairline.copy(alpha = alpha),
                        KanColors.Hairline.copy(alpha = alpha * 0.6f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
fun CeremonialMetric(
    label: String,
    primary: String,
    secondary: String,
    support: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(label)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = primary,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = (-1.4).sp,
                color = KanColors.TextPrimary,
                lineHeight = 46.sp,
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
        Spacer(Modifier.height(14.dp))
        Text(
            text = support.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = KanColors.TextSecondary,
            lineHeight = 15.sp,
        )
    }
}

