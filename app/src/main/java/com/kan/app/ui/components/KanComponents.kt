package com.kan.app.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kan.app.ui.theme.KanColors

@Composable
fun KanScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KanColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp, vertical = 56.dp),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
        color = KanColors.TextTertiary,
    )
}

@Composable
fun Hairline(alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KanColors.Hairline.copy(alpha = alpha)),
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
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = (-1.2).sp,
                color = KanColors.TextPrimary,
                lineHeight = 44.sp,
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
        Text(
            text = support.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.7.sp,
            color = KanColors.TextSecondary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
