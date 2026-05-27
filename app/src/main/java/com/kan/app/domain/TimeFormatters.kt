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
    val seconds = duration.minusHours(hours).minusMinutes(minutes).seconds
    return when {
        hours > 0L -> "%dh %02dm %02ds".format(hours, minutes, seconds)
        minutes > 0L -> "%dm %02ds".format(minutes, seconds)
        else -> "${seconds}s"
    }
}
