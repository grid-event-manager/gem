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

class LiveProofCommandTest {
    @Test
    fun `missing live proof inputs write blocked report before send`() {
        val directory = Files.createTempDirectory("hostess-live-proof-missing")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--report",
                    reportPath.toString(),
                    "--authorised-live-send",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("missing required live proof input") })
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"step\": \"validate-inputs\"")
            assertContains(report, "\"state\": \"blocked\"")
            assertContains(report, "grid")
            assertContains(report, "\"cleanupStatus\": \"not_applicable\"")
            assertFalse(RAW_UUID.containsMatchIn(report))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `complete live proof inputs reach shared runtime and fail closed without leaking inputs`() {
        val directory = Files.createTempDirectory("hostess-live-proof-unavailable")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--report",
                    reportPath.toString(),
                    "--authorised-live-send",
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
                    "--target",
                    "Venue Hosts",
                    "--subject",
                    "Tonight",
                    "--body",
                    "Doors at eight",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertTrue(output.lines.any { it == "live-proof live failed: [redacted]" })
            assertContains(report, "\"status\": \"failed\"")
            assertContains(report, "\"account\": \"[redacted]\"")
            assertContains(report, "\"credentialHandle\": \"[redacted]\"")
            assertContains(report, "\"step\": \"login\"")
            assertContains(report, "\"state\": \"failed\"")
            assertContains(report, "\"step\": \"list-groups\"")
            assertContains(report, "\"state\": \"not_run\"")
            assertFalse(report.contains("venue-proof"))
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
            assertFalse(RAW_UUID.containsMatchIn(report))
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
