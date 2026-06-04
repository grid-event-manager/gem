package org.hostess.tools.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.hostess.tools.cli.composition.CliCompositionRoot

class CommandRegistryTest {
    @Test
    fun `default registry exposes required proof commands`() {
        val registry = CommandRegistry.default(CliCompositionRoot())

        assertEquals(setOf("list-groups", "live-proof", "login", "send-notice"), registry.commandNames)
    }

    @Test
    fun `fake list groups command renders display names without raw group IDs`() {
        val output = RecordingCliOutput()
        val exitCode = CommandRegistry.default(CliCompositionRoot())
            .execute(listOf("list-groups", "--mode", "fake"), output)

        assertEquals(0, exitCode)
        assertTrue(output.lines.any { it.contains("Venue Hosts") })
        assertTrue(output.lines.any { it.contains("Event Notices") })
        assertFalse(output.lines.any { it.contains("fake-group") })
    }

    @Test
    fun `invalid mode returns usage error`() {
        val output = RecordingCliOutput()
        val exitCode = CommandRegistry.default(CliCompositionRoot())
            .execute(listOf("list-groups", "--mode", "mystery"), output)

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("Unknown mode") })
    }
}

class RecordingCliOutput : CliOutput {
    val lines = mutableListOf<String>()

    override fun line(value: String) {
        lines += value
    }
}
