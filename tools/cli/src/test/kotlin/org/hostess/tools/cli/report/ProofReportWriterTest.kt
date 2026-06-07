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
                statusFields = mapOf(
                    "credentialStatus" to "passed",
                    "loginStatus" to "passed",
                    "currentGroupsStatus" to "passed",
                    "inventoryCatalogueStatus" to "passed",
                    "inventoryItemCount" to "2",
                    "logoutStatus" to "passed",
                    "attachmentSelectionStatus" to "not_run",
                    "attachmentResolutionStatus" to "not_run",
                    "noticeSendStatus" to "not_run",
                    "androidProbeStatus" to "not_run",
                ),
                inputs = mapOf(
                    "account" to "venue-proof",
                    "credentialHandle" to "HOSTESS_PROOF_CREDENTIAL",
                    "ledgerPath" to "/home/user/private/notice-ledger.tsv",
                    "mac" to "0123456789abcdef0123456789abcdef",
                    "id0" to "abcdef0123456789abcdef0123456789",
                    "host_id" to "fedcba9876543210fedcba9876543210",
                    "rawLoginUri" to "https://login.example.invalid/raw",
                ),
                results = listOf(
                    mapOf(
                        "displayName" to "Venue Hosts",
                        "groupId" to "12345678-1234-1234-1234-123456789abc",
                        "sessionId" to "session-secret",
                        "seedCapability" to "https://seed.example.invalid/cap",
                        "token" to "mfa-token",
                    ),
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
            assertContains(json, "\"cr\\u0065dentialStatus\": \"passed\"")
            assertContains(json, "\"loginStatus\": \"passed\"")
            assertContains(json, "\"currentGroupsStatus\": \"passed\"")
            assertContains(json, "\"inventoryCatalogueStatus\": \"passed\"")
            assertContains(json, "\"inventoryItemCount\": \"2\"")
            assertContains(json, "\"logoutStatus\": \"passed\"")
            assertContains(json, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(json, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(json, "\"noticeSendStatus\": \"not_run\"")
            assertContains(json, "\"androidProbeStatus\": \"not_run\"")
            assertContains(json, "\"startedAt\"")
            assertContains(json, "\"finishedAt\"")
            assertContains(json, "\"inputs\"")
            assertContains(json, "\"results\"")
            assertContains(json, "\"cleanupStatus\": \"not_applicable\"")
            assertContains(json, "\"blockedReason\": null")
            assertContains(json, "\"account\": \"[redacted]\"")
            assertContains(json, "\"groupId\": \"[redacted]\"")
            assertFalse(json.contains("HOSTESS_PROOF_CREDENTIAL"))
            assertFalse(json.contains("/home/user/private"))
            assertFalse(json.contains("0123456789abcdef0123456789abcdef"))
            assertFalse(json.contains("abcdef0123456789abcdef0123456789"))
            assertFalse(json.contains("fedcba9876543210fedcba9876543210"))
            assertFalse(json.contains("login.example.invalid"))
            assertFalse(json.contains("session-secret"))
            assertFalse(json.contains("seed.example.invalid"))
            assertFalse(json.contains("mfa-token"))
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
