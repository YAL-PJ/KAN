package com.kan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object KanColors {
    val Void = Color(0xFF04060A)
    val VoidElevated = Color(0xFF0A0D13)
    val VoidGlass = Color(0xFF0E1219)

    val SteelDeep = Color(0xFF2B2F36)
    val SteelShadow = Color(0xFF5E646D)
    val Steel = Color(0xFFC2C8D1)
    val SteelHighlight = Color(0xFFF1F4F8)

    val TextPrimary = Color(0xFFE9ECF1)
    val TextSecondary = Color(0xFFA0A7B0)
    val TextTertiary = Color(0xFF6B7178)
    val TextMuted = Color(0xFF454A52)
    val Hairline = Color(0xFF24272D)

    val PrismRed = Color(0xFFE85D4A)
    val PrismGold = Color(0xFFF2B23A)
    val PrismGreen = Color(0xFF4DD49A)
    val PrismBlue = Color(0xFF4C8DE8)
    val PrismViolet = Color(0xFFA371E3)

    val Accent = Steel
    val Background = Void
    val Surface = VoidElevated
}

@Composable
fun KanTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = KanColors.Void,
        surface = KanColors.VoidElevated,
        onBackground = KanColors.TextPrimary,
        onSurface = KanColors.TextPrimary,
        primary = KanColors.Steel,
        onPrimary = KanColors.Void,
        secondary = KanColors.TextSecondary,
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
