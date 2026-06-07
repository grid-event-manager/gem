package org.hostess.tools.cli.commands

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.hostess.tools.cli.CommandRegistry
import org.hostess.tools.cli.RecordingCliOutput
import org.hostess.tools.cli.composition.CliCompositionRoot

class SendNoticeCommandTest {
    @Test
    fun `missing mode is usage error and does not run fake send`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "send-notice",
                "--target",
                "Venue Hosts",
                "--subject",
                "Tonight",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("mode is required") })
        assertFalse(output.lines.any { it.contains("send-notice fake") })
    }

    @Test
    fun `live send notice is blocked before usage validation or runtime work`() {
        withReport { reportPath ->
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "send-notice",
                    "--mode",
                    "live",
                    "--attachment-kind",
                    "texture",
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertEquals(
                listOf("send-notice live blocked: live send is only available through live-proof --proof-scope full"),
                output.lines,
            )
            assertContains(report, "\"command\": \"send-notice\"")
            assertContains(report, "\"mode\": \"live\"")
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"sendNoticeStatus\": \"blocked\"")
            assertContains(report, "\"targetCount\": \"0\"")
            assertContains(report, "\"subjectLength\": \"0\"")
            assertContains(report, "\"bodyLength\": \"0\"")
            assertContains(report, "\"blockedReason\": \"live send is only available through live-proof --proof-scope full\"")
            assertFalse(report.contains("targetDisplayNames"))
            assertFalse(report.contains("attachment-id"))
        }
    }

    @Test
    fun `live send notice blocks valid looking mutation before attachment resolution`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "send-notice",
                "--mode",
                "live",
                "--target",
                "Venue Hosts",
                "--subject",
                "Tonight",
                "--body",
                "Doors at eight",
                "--attachment-kind",
                "landmark",
            ),
            output,
        )

        assertEquals(3, exitCode)
        assertEquals(
            listOf("send-notice live blocked: live send is only available through live-proof --proof-scope full"),
            output.lines,
        )
    }

    @Test
    fun `fake send notice defaults compliance projection and writes status fields`() {
        withReport { reportPath ->
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "send-notice",
                    "--mode",
                    "fake",
                    "--target",
                    "Venue Hosts",
                    "--subject",
                    "Tonight",
                    "--body",
                    "Doors at eight",
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(0, exitCode)
            assertTrue(output.lines.any { it == "send-notice fake attempted=1" })
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"noticeComplianceStatus\": \"passed\"")
            assertContains(report, "\"noticeSubmissionProjectionStatus\": \"passed\"")
            assertContains(report, "\"noticeSubmissionsProjected\": \"1\"")
            assertContains(report, "\"noticeSubmissionLedgerGroupCount\": \"1\"")
            assertContains(report, "\"noticeSubmissionLedgerMaxGroupTotal\": \"1\"")
            assertContains(report, "\"noticeSubmissionPerGroupHardCap\": \"180\"")
            assertContains(report, "\"noticeLedgerConfigured\": \"true\"")
        }
    }

    @Test
    fun `stale recipient count option is rejected before attachment resolution`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "send-notice",
                "--mode",
                "fake",
                "--target",
                "Venue Hosts",
                "--subject",
                "Tonight",
                "--body",
                "Doors at eight",
                "--operator",
                "test-operator",
                "--recipient-count",
                "Event Notices=10",
                "--attachment-kind",
                "texture",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("recipient-count is no longer supported; notice submissions are derived from selected target groups") })
        assertFalse(output.lines.any { it.contains("missing attachment-id") })
    }

    @Test
    fun `stale recipient count source option is rejected in fake mode too`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "send-notice",
                "--mode",
                "fake",
                "--target",
                "Venue Hosts",
                "--subject",
                "Tonight",
                "--body",
                "Doors at eight",
                "--operator",
                "test-operator",
                "--recipient-count-source",
                "operator-acknowledged",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("recipient-count-source is no longer supported; notice submissions are derived from selected target groups") })
    }

    @Test
    fun `explicit notice submission report omits ledger path`() {
        val directory = Files.createTempDirectory("hostess-send-notice")
        try {
            val reportPath = directory.resolve("send-notice.json")
            val ledgerPath = directory.resolve("notice-ledger.tsv")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "send-notice",
                    "--mode",
                    "fake",
                    "--target",
                    "Venue Hosts",
                    "--subject",
                    "Tonight",
                    "--body",
                    "Doors at eight",
                    "--operator",
                    "test-operator",
                    "--ledger",
                    ledgerPath.toString(),
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(0, exitCode)
            assertContains(report, "\"noticeSubmissionsProjected\": \"1\"")
            assertContains(report, "\"noticeOperator\": \"test-operator\"")
            assertFalse(report.contains("notice-ledger.tsv"))
            assertFalse(report.contains(ledgerPath.toString()))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("hostess-send-notice")
        try {
            assertion(directory.resolve("send-notice.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
