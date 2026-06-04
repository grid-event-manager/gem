package org.hostess.tools.cli.report

@JvmInline
value class RedactedText private constructor(val value: String) {
    companion object {
        private val groupUuidPattern = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        )
        private val bearerPattern = Regex("Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE)
        private val seedCapPattern = Regex("seed[_-]?cap=[^\\s&]+", RegexOption.IGNORE_CASE)
        private val uploadUrlPattern = Regex("upload_url=[^\\s&]+", RegexOption.IGNORE_CASE)
        private val sensitiveKeys = setOf(
            "account",
            "accountLabel",
            "credential",
            "credentialHandle",
            "credentialEnv",
            "credentialFile",
            "groupId",
            "password",
            "passwd",
            "seed_cap",
            "seedcap",
            "session_id",
            "sessionId",
            "upload_url",
            "uploadUrl",
            "token",
        )

        fun from(key: String, rawValue: String): RedactedText {
            if (sensitiveKeys.any { it.equals(key, ignoreCase = true) }) {
                return RedactedText("[redacted]")
            }

            return RedactedText(
                rawValue
                    .replace(groupUuidPattern, "[redacted-group-id]")
                    .replace(bearerPattern, "Bearer [redacted]")
                    .replace(seedCapPattern, "seed_cap=[redacted]")
                    .replace(uploadUrlPattern, "upload_url=[redacted]"),
            )
        }

        fun map(values: Map<String, String>): Map<String, String> =
            values.mapValues { (key, value) -> from(key, value).value }
    }
}
