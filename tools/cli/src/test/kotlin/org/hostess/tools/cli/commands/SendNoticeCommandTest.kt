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
            assertContains(report, "\"recipientProjectionStatus\": \"passed\"")
            assertContains(report, "\"recipientDeliveryProjected\": \"1\"")
            assertContains(report, "\"recipientDeliveryLedgerTotal\": \"1\"")
            assertContains(report, "\"recipientDeliveryHardCap\": \"4500\"")
            assertContains(report, "\"noticeLedgerConfigured\": \"true\"")
        }
    }

    @Test
    fun `recipient counts are matched by selected display name before attachment resolution`() {
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
                "--recipient-count-source",
                "operator-acknowledged",
                "--attachment-kind",
                "texture",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("recipient-count must name only selected groups") })
        assertFalse(output.lines.any { it.contains("missing attachment-id") })
    }

    @Test
    fun `duplicate recipient count for selected display name is a usage error`() {
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
                "Venue Hosts=10",
                "--recipient-count",
                "Venue Hosts=11",
                "--recipient-count-source",
                "authoritative",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("recipient-count must include exactly one entry per selected group") })
    }

    @Test
    fun `unsupported recipient count source is a usage error in fake mode too`() {
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
                "Venue Hosts=10",
                "--recipient-count-source",
                "spreadsheet-ish",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("recipient-count-source must be operator-acknowledged or authoritative") })
    }

    @Test
    fun `explicit recipient count report omits ledger path`() {
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
                    "--recipient-count",
                    "Venue Hosts=12",
                    "--recipient-count-source",
                    "operator-acknowledged",
                    "--ledger",
                    ledgerPath.toString(),
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(0, exitCode)
            assertContains(report, "\"recipientDeliveryProjected\": \"12\"")
            assertContains(report, "\"noticeOperator\": \"test-operator\"")
            assertContains(report, "\"recipientCountSource\": \"operator-acknowledged\"")
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
