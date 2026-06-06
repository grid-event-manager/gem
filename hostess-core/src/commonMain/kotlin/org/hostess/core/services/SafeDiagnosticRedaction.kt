package org.hostess.core.services

import org.hostess.core.ports.RedactionPort

object SafeDiagnosticRedaction {
    private val uuidPattern = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    )
    private val bearerPattern = Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE)
    private val seedCapPattern = Regex("seed[_-]?cap=[^\\s&]+", RegexOption.IGNORE_CASE)
    private val urlPattern = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val sensitiveAssignmentPattern = Regex(
        "\\b(passwd|password|secret|token|session[_-]?id|seed[_-]?capability|mac|id0|host[_-]?id)" +
            "\\s*[:=]\\s*[^,;\\s]+",
        RegexOption.IGNORE_CASE,
    )
    private val sensitiveKeys = setOf(
        "account",
        "accountLabel",
        "circuitCode",
        "credential",
        "credentialHandle",
        "credentialEnv",
        "credentialFile",
        "eventQueueUrl",
        "existingAttachmentId",
        "groupId",
        "host_id",
        "hostId",
        "id0",
        "ledgerPath",
        "localPath",
        "mac",
        "password",
        "passwd",
        "rawLoginUri",
        "recipientCountPath",
        "retentionNote",
        "seed_cap",
        "seedCapability",
        "seedcap",
        "session_id",
        "sessionId",
        "simulatorEndpoint",
        "simulatorIp",
        "simulatorPort",
        "token",
    )

    fun redact(rawValue: String): String = redact("detail", rawValue)

    fun redact(key: String, rawValue: String): String {
        if (sensitiveKeys.any { it.equals(key, ignoreCase = true) }) {
            return REDACTED
        }

        return rawValue
            .replace(bearerPattern, "Bearer $REDACTED")
            .replace(seedCapPattern, "seed_cap=$REDACTED")
            .replace(sensitiveAssignmentPattern) { match ->
                val name = match.groupValues[1]
                "$name=$REDACTED"
            }
            .replace(urlPattern, "[redacted-url]")
            .replace(uuidPattern, "[redacted-id]")
    }

    fun redactMap(values: Map<String, String>): Map<String, String> =
        values.mapValues { (key, value) -> redact(key, value) }

    fun excerpt(rawValue: String, maxLength: Int = DEFAULT_EXCERPT_LENGTH): String {
        val redacted = redact(rawValue)
            .replace(Regex("\\s+"), " ")
            .trim()
        if (redacted.length <= maxLength) {
            return redacted
        }
        return redacted.take(maxLength).trimEnd() + "..."
    }

    private const val REDACTED = "[redacted]"
    private const val DEFAULT_EXCERPT_LENGTH = 500
}

object DefaultRedactionPort : RedactionPort {
    override fun redact(value: String): String = SafeDiagnosticRedaction.redact(value)
}
