package com.kan.app.data

import android.content.Context
import java.io.File

internal class UsageLogStore(context: Context, private val limit: Int) {
    private val phoneSessionsFile = File(context.filesDir, PHONE_SESSIONS_FILE_NAME)
    private val challengeAttemptsFile = File(context.filesDir, CHALLENGE_ATTEMPTS_FILE_NAME)

    private var phoneSessionsCache: List<PhoneSessionEntry>? = null
    private var challengeAttemptsCache: List<ChallengeAttemptEntry>? = null

    fun readPhoneSessions(): List<PhoneSessionEntry> = phoneSessionsCache ?: phoneSessionsFile
        .readRecentLines(limit)
        .mapNotNull(UsageLogSerializer::decodePhoneSessionLine)
        .asReversed()
        .also { phoneSessionsCache = it }

    fun readChallengeAttempts(): List<ChallengeAttemptEntry> = challengeAttemptsCache ?: challengeAttemptsFile
        .readRecentLines(limit)
        .mapNotNull(UsageLogSerializer::decodeChallengeAttemptLine)
        .asReversed()
        .also { challengeAttemptsCache = it }

    fun appendPhoneSession(entry: PhoneSessionEntry) {
        val previous = readPhoneSessions()
        phoneSessionsFile.appendEntry(UsageLogSerializer.encodePhoneSessionLine(entry))
        phoneSessionsCache = (listOf(entry) + previous).take(limit)
        phoneSessionsFile.trimToRecentLines(limit)
    }

    fun appendChallengeAttempt(entry: ChallengeAttemptEntry) {
        val previous = readChallengeAttempts()
        challengeAttemptsFile.appendEntry(UsageLogSerializer.encodeChallengeAttemptLine(entry))
        challengeAttemptsCache = (listOf(entry) + previous).take(limit)
        challengeAttemptsFile.trimToRecentLines(limit)
    }

    fun importPhoneSessions(entriesNewestFirst: List<PhoneSessionEntry>) {
        if (entriesNewestFirst.isEmpty() || phoneSessionsFile.length() > 0L) return
        phoneSessionsFile.parentFile?.mkdirs()
        phoneSessionsFile.writeText(
            entriesNewestFirst
                .take(limit)
                .asReversed()
                .joinToString(separator = "\n", postfix = "\n", transform = UsageLogSerializer::encodePhoneSessionLine),
        )
        phoneSessionsCache = entriesNewestFirst.take(limit)
    }

    fun importChallengeAttempts(entriesNewestFirst: List<ChallengeAttemptEntry>) {
        if (entriesNewestFirst.isEmpty() || challengeAttemptsFile.length() > 0L) return
        challengeAttemptsFile.parentFile?.mkdirs()
        challengeAttemptsFile.writeText(
            entriesNewestFirst
                .take(limit)
                .asReversed()
                .joinToString(separator = "\n", postfix = "\n", transform = UsageLogSerializer::encodeChallengeAttemptLine),
        )
        challengeAttemptsCache = entriesNewestFirst.take(limit)
    }

    private fun File.appendEntry(line: String) {
        parentFile?.mkdirs()
        appendText("$line\n")
    }

    private fun File.readRecentLines(maxLines: Int): List<String> {
        if (!exists()) return emptyList()
        return readLines().takeLast(maxLines)
    }

    private fun File.trimToRecentLines(maxLines: Int) {
        val lines = readRecentLines(maxLines + 1)
        if (lines.size <= maxLines) return
        writeText(lines.takeLast(maxLines).joinToString(separator = "\n", postfix = "\n"))
    }

    private companion object {
        const val PHONE_SESSIONS_FILE_NAME = "kan_phone_sessions.log"
        const val CHALLENGE_ATTEMPTS_FILE_NAME = "kan_challenge_attempts.log"
    }
}
