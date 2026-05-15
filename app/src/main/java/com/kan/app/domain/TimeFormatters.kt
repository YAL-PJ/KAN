package com.kan.app.domain

import java.time.Duration

fun Long.toClockTime(): String {
    val safeSeconds = coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    val seconds = safeSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun Long.toHumanDuration(): String {
    val duration = Duration.ofSeconds(coerceAtLeast(0L))
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m"
        else -> "${duration.seconds}s"
    }
}
