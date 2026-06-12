package org.gem.tools.cli.commands

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gem.tools.cli.CommandRegistry
import org.gem.tools.cli.RecordingCliOutput
import org.gem.tools.cli.composition.CliCompositionRoot

class LoginCommandTest {
    @Test
    fun `missing mode is usage error and does not run fake login`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "login",
                "--report",
                "login.json",
            ),
            output,
        )

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("mode is required") })
        assertFalse(output.lines.any { it.contains("login fake ready") })
    }

    @Test
    fun `fake login passes through compliance and writes redacted status fields`() {
        val directory = Files.createTempDirectory("hostess-login-fake")
        try {
            val reportPath = directory.resolve("login.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "login",
                    "--mode",
                    "fake",
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(0, exitCode)
            assertTrue(output.lines.any { it == "login fake ready for fake-account" })
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"proofAccountStatus\": \"passed\"")
            assertContains(report, "\"scriptedAgentStatus\": \"passed\"")
            assertContains(report, "\"operatorStatus\": \"passed\"")
            assertContains(report, "\"account\": \"[redacted]\"")
            assertContains(report, "\"proofAccountLabel\": \"fake-proof-account\"")
            assertFalse(report.contains("fake-credential"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `live login missing compliance evidence writes blocked report before login`() {
        val directory = Files.createTempDirectory("hostess-login-compliance-block")
        try {
            val reportPath = directory.resolve("login.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "login",
                    "--mode",
                    "live",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("missing login compliance input") })
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"loginComplianceStatus\": \"blocked\"")
            assertContains(report, "\"proofAccountStatus\": \"blocked\"")
            assertContains(report, "\"scriptedAgentStatus\": \"blocked\"")
            assertContains(report, "\"operatorStatus\": \"blocked\"")
            assertContains(report, "proof-account-attested")
            assertContains(report, "operator")
            assertContains(report, "\"account\": \"[redacted]\"")
            assertFalse(report.contains("venue-proof"))
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
            assertFalse(report.contains("fake-proof-account"))
            assertFalse(report.contains("fake-operator"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `live login requires scripted agent evidence even when automated use false`() {
        val directory = Files.createTempDirectory("hostess-login-scripted-evidence-block")
        try {
            val reportPath = directory.resolve("login.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "login",
                    "--mode",
                    "live",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
                    "--report",
                    reportPath.toString(),
                    "--proof-account-attested",
                    "--automated-use",
                    "false",
                    "--operator",
                    "test-operator",
                    "--proof-account-label",
                    "test-proof-account",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("scripted-agent-attested") })
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"loginComplianceStatus\": \"blocked\"")
            assertContains(report, "\"proofAccountStatus\": \"passed\"")
            assertContains(report, "\"scriptedAgentStatus\": \"blocked\"")
            assertFalse(report.contains("venue-proof"))
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
