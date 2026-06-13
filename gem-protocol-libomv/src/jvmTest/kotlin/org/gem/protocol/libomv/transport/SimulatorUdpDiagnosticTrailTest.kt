package org.gem.protocol.libomv.transport

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulatorUdpDiagnosticTrailTest {
    @Test
    fun `diagnostic trail is disabled by default`() {
        val tempDir = Files.createTempDirectory("gem-udp-diagnostics-disabled").toFile()
        val previousEnabled = System.getProperty(ENABLED_PROPERTY)
        val previousPath = System.getProperty(PATH_PROPERTY)

        try {
            System.clearProperty(ENABLED_PROPERTY)
            System.setProperty(PATH_PROPERTY, tempDir.absolutePath)
            SimulatorUdpDiagnosticTrail.closeForTests()

            SimulatorUdpDiagnosticTrail.record("test_disabled", "field" to "value")
            SimulatorUdpDiagnosticTrail.closeForTests()

            assertTrue(tempDir.listFiles().isNullOrEmpty())
        } finally {
            restoreProperty(ENABLED_PROPERTY, previousEnabled)
            restoreProperty(PATH_PROPERTY, previousPath)
            SimulatorUdpDiagnosticTrail.closeForTests()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `diagnostic trail writes sanitized metadata when explicitly enabled`() {
        val tempDir = Files.createTempDirectory("gem-udp-diagnostics-enabled").toFile()
        val previousEnabled = System.getProperty(ENABLED_PROPERTY)
        val previousPath = System.getProperty(PATH_PROPERTY)

        try {
            System.setProperty(ENABLED_PROPERTY, "true")
            System.setProperty(PATH_PROPERTY, tempDir.absolutePath)
            SimulatorUdpDiagnosticTrail.closeForTests()

            SimulatorUdpDiagnosticTrail.record(
                "test_event",
                "unsafe" to "line one\nline two\tline three",
                "packetType" to SimulatorPacketType.PACKET_ACK.name,
            )
            SimulatorUdpDiagnosticTrail.closeForTests()

            val logs = tempDir.listFiles().orEmpty().filter(File::isFile)
            assertEquals(1, logs.size)
            val lines = logs.single().readLines()
            assertTrue(lines.any { it.contains("event=test_event") })
            val eventLine = lines.single { it.contains("event=test_event") }
            assertTrue(eventLine.contains("unsafe=line one line two line three"))
            assertFalse(eventLine.contains('\t'))
        } finally {
            restoreProperty(ENABLED_PROPERTY, previousEnabled)
            restoreProperty(PATH_PROPERTY, previousPath)
            SimulatorUdpDiagnosticTrail.closeForTests()
            tempDir.deleteRecursively()
        }
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }

    private companion object {
        const val ENABLED_PROPERTY = "gem.simulator.udp.diagnostics"
        const val PATH_PROPERTY = "gem.simulator.udp.diagnostics.path"
    }
}
