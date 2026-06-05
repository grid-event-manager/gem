package org.hostess.tools.cli

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.hostess.tools.cli.composition.CliCompositionRoot

class CliCompositionRootTest {
    @Test
    fun `fake send notice command reaches core dispatch path`() {
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
            ),
            output,
        )

        assertEquals(0, exitCode)
        assertTrue(output.lines.any { it == "send-notice fake attempted=1" })
        assertTrue(output.lines.any { it == "Venue Hosts: sent" })
    }

    @Test
    fun `live proof validates credential handle before live execution`() {
        val directory = Files.createTempDirectory("hostess-live-proof-composition")
        try {
            val output = RecordingCliOutput()
            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--report",
                    directory.resolve("live-proof.json").toString(),
                    "--account",
                    "venue-proof",
                ),
                output,
            )

            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("credential-env") })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
