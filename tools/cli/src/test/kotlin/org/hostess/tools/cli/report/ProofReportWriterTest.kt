package org.hostess.tools.cli.report

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProofReportWriterTest {
    @Test
    fun `writes stable proof report schema fields`() {
        val directory = Files.createTempDirectory("hostess-proof-report")
        try {
            val reportPath = directory.resolve("report.json")

            val report = ProofReportWriter().writeIfRequested(
                reportPath = reportPath.toString(),
                command = "list-groups",
                mode = "fake",
                status = ProofReportStatus.PASSED,
                statusFields = mapOf("plainNoticeStatus" to "passed"),
                inputs = mapOf("account" to "venue-proof"),
                results = listOf(
                    mapOf("displayName" to "Venue Hosts", "groupId" to "12345678-1234-1234-1234-123456789abc"),
                ),
                cleanupStatus = "not_applicable",
            )

            val json = reportPath.readText()
            assertNotNull(report)
            assertContains(json, "\"schemaVersion\"")
            assertContains(json, "\"runId\"")
            assertContains(json, "\"command\": \"list-groups\"")
            assertContains(json, "\"mode\": \"fake\"")
            assertContains(json, "\"status\": \"passed\"")
            assertContains(json, "\"plainNoticeStatus\": \"passed\"")
            assertContains(json, "\"startedAt\"")
            assertContains(json, "\"finishedAt\"")
            assertContains(json, "\"inputs\"")
            assertContains(json, "\"results\"")
            assertContains(json, "\"cleanupStatus\": \"not_applicable\"")
            assertContains(json, "\"blockedReason\": null")
            assertContains(json, "\"account\": \"[redacted]\"")
            assertContains(json, "\"groupId\": \"[redacted]\"")
            assertFalse(RAW_UUID.containsMatchIn(json))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private companion object {
        val RAW_UUID = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        )
    }
}
