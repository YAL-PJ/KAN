package com.kan.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PhoneSessionEntry(
    val startedAtMillis: Long,
    val endedAtMillis: Long,
) {
    val durationSeconds: Long
        get() = ((endedAtMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)

    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startedAtMillis).atZone(zoneId).toLocalDate()
}

data class ChallengeAttemptEntry(
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val plannedDurationSeconds: Long,
    val successful: Boolean,
) {
    val elapsedSeconds: Long
        get() = ((endedAtMillis - startedAtMillis) / 1_000L).coerceAtLeast(0L)

    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startedAtMillis).atZone(zoneId).toLocalDate()
}
