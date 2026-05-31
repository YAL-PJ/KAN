package com.kan.app.data

internal object UsageLogSerializer {
    private const val ENTRY_DELIMITER = '\n'
    private const val FIELD_DELIMITER = '|'
    private const val PHONE_SESSION_FIELD_COUNT = 2
    private const val CHALLENGE_ATTEMPT_FIELD_COUNT = 4

    fun encodePhoneSessions(entries: List<PhoneSessionEntry>, limit: Int): String = entries
        .take(limit)
        .joinToString(ENTRY_DELIMITER.toString(), transform = ::encodePhoneSessionLine)

    fun encodePhoneSessionLine(entry: PhoneSessionEntry): String =
        listOf(entry.startedAtMillis, entry.endedAtMillis).joinToString(FIELD_DELIMITER.toString())

    fun decodePhoneSessions(raw: String?, limit: Int): List<PhoneSessionEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(ENTRY_DELIMITER)
            .mapNotNull(::decodePhoneSessionLine)
            .take(limit)
    }

    fun encodeChallengeAttempts(entries: List<ChallengeAttemptEntry>, limit: Int): String = entries
        .take(limit)
        .joinToString(ENTRY_DELIMITER.toString(), transform = ::encodeChallengeAttemptLine)

    fun encodeChallengeAttemptLine(entry: ChallengeAttemptEntry): String = listOf(
        entry.startedAtMillis,
        entry.endedAtMillis,
        entry.plannedDurationSeconds,
        entry.successful,
    ).joinToString(FIELD_DELIMITER.toString())

    fun decodeChallengeAttempts(raw: String?, limit: Int): List<ChallengeAttemptEntry> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(ENTRY_DELIMITER)
            .mapNotNull(::decodeChallengeAttemptLine)
            .take(limit)
    }

    fun decodePhoneSessionLine(line: String): PhoneSessionEntry? {
        val parts = line.split(FIELD_DELIMITER)
        if (parts.size != PHONE_SESSION_FIELD_COUNT) return null
        val startedAt = parts[0].toLongOrNull() ?: return null
        val endedAt = parts[1].toLongOrNull() ?: return null
        if (startedAt <= 0L || endedAt < startedAt) return null
        return PhoneSessionEntry(startedAtMillis = startedAt, endedAtMillis = endedAt)
    }

    fun decodeChallengeAttemptLine(line: String): ChallengeAttemptEntry? {
        val parts = line.split(FIELD_DELIMITER)
        if (parts.size != CHALLENGE_ATTEMPT_FIELD_COUNT) return null
        val startedAt = parts[0].toLongOrNull() ?: return null
        val endedAt = parts[1].toLongOrNull() ?: return null
        if (startedAt <= 0L || endedAt < startedAt) return null
        return ChallengeAttemptEntry(
            startedAtMillis = startedAt,
            endedAtMillis = endedAt,
            plannedDurationSeconds = parts[2].toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            successful = parts[3].toBooleanStrictOrNull() ?: false,
        )
    }
}
