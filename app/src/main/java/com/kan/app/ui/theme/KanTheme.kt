package com.kan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object KanColors {
    val Background = Color(0xFF080A0D)
    val Surface = Color(0xFF0D1014)
    val TextPrimary = Color(0xFFE7E1D5)
    val TextSecondary = Color(0xFFB7AFA2)
    val TextTertiary = Color(0xFF7E766B)
    val TextMuted = Color(0xFF565047)
    val Hairline = Color(0xFF2B3037)
    val Accent = Color(0xFFC2A66B)
}

@Composable
fun KanTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = KanColors.Background,
        surface = KanColors.Surface,
        onBackground = KanColors.TextPrimary,
        onSurface = KanColors.TextPrimary,
        primary = KanColors.Accent,
        onPrimary = KanColors.Background,
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
