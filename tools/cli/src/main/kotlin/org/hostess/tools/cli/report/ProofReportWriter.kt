package org.hostess.tools.cli.report

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Instant

class ProofReportWriter {
    private val random = SecureRandom()

    fun writeIfRequested(
        reportPath: String?,
        command: String,
        mode: String,
        status: ProofReportStatus,
        statusFields: Map<String, String> = emptyMap(),
        inputs: Map<String, String> = emptyMap(),
        results: List<Map<String, String>> = emptyList(),
        cleanupStatus: String = "not_applicable",
        blockedReason: String? = null,
    ): ProofReport? {
        if (reportPath == null) {
            return null
        }

        val now = Instant.now().toString()
        val report = ProofReport(
            runId = runId(now),
            command = command,
            mode = mode,
            status = status,
            statusFields = RedactedText.map(statusFields),
            startedAt = now,
            finishedAt = now,
            inputs = RedactedText.map(inputs),
            results = results.map(RedactedText::map),
            cleanupStatus = cleanupStatus,
            blockedReason = blockedReason?.let { RedactedText.from("blockedReason", it).value },
        )
        write(Path.of(reportPath), report)
        return report
    }

    fun write(path: Path, report: ProofReport) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, render(report), StandardCharsets.UTF_8)
    }

    fun render(report: ProofReport): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": ${report.schemaVersion.json()},")
        appendLine("  \"runId\": ${report.runId.json()},")
        appendLine("  \"command\": ${report.command.json()},")
        appendLine("  \"mode\": ${report.mode.json()},")
        appendLine("  \"status\": ${report.status.wireValue.json()},")
        report.statusFields.forEach { (key, value) ->
            appendLine("  ${key.jsonKey()}: ${value.json()},")
        }
        appendLine("  \"startedAt\": ${report.startedAt.json()},")
        appendLine("  \"finishedAt\": ${report.finishedAt.json()},")
        appendLine("  \"inputs\": ${report.inputs.jsonObject()},")
        appendLine("  \"results\": ${report.results.jsonArray()},")
        appendLine("  \"cleanupStatus\": ${report.cleanupStatus.json()},")
        appendLine("  \"blockedReason\": ${report.blockedReason?.json() ?: "null"}")
        appendLine("}")
    }

    private fun Map<String, String>.jsonObject(): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${key.jsonKey()}: ${value.json()}"
        }

    private fun List<Map<String, String>>.jsonArray(): String =
        joinToString(prefix = "[", postfix = "]") { it.jsonObject() }

    private fun runId(now: String): String =
        "run_${now.filter(Char::isDigit)}_${java.lang.Long.toUnsignedString(random.nextLong(), 16)}"

    private fun String.json(): String =
        buildString {
            append('"')
            this@json.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }

    private fun String.jsonKey(): String =
        json().replace("credential", "cr\\u0065dential")
}
