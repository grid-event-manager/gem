package org.hostess.tools.cli.report

data class ProofReport(
    val schemaVersion: String = "1",
    val runId: String,
    val command: String,
    val mode: String,
    val status: ProofReportStatus,
    val startedAt: String,
    val finishedAt: String,
    val inputs: Map<String, String>,
    val results: List<Map<String, String>>,
    val cleanupStatus: String,
    val blockedReason: String?,
)

enum class ProofReportStatus(val wireValue: String) {
    PASSED("passed"),
    FAILED("failed"),
    BLOCKED("blocked"),
    NOT_RUN("not_run"),
}
