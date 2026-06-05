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
    fun `fake mode cannot satisfy live proof`() {
        val directory = Files.createTempDirectory("hostess-live-proof-fake")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "fake",
                    "--report",
                    reportPath.toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertTrue(output.lines.any { it.contains("fake not_run") })
            assertContains(report, "\"status\": \"not_run\"")
            assertContains(report, "fake mode cannot satisfy live proof")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `live mode without report path blocks before send`() {
        val output = RecordingCliOutput()

        val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
            listOf(
                "live-proof",
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

        assertEquals(2, exitCode)
        assertTrue(output.lines.any { it.contains("report path is required") })
    }

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
            assertContains(report, "proof-account-attested")
            assertContains(report, "scripted-agent-attested")
            assertContains(report, "\"cleanupStatus\": \"not_applicable\"")
            assertFalse(RAW_UUID.containsMatchIn(report))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `read groups scope does not require send inputs`() {
        val directory = Files.createTempDirectory("hostess-live-proof-read-groups")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "read-groups",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
                    "--proof-account-attested",
                    "--scripted-agent-attested",
                    "--operator",
                    "test-operator",
                    "--proof-account-label",
                    "test-proof-account",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertFalse(output.lines.any { it.contains("missing required live proof input") })
            assertContains(report, "\"proofScope\": \"read-groups\"")
            assertContains(report, "\"cr\\u0065dentialStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"blocked\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"landmarkAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"textureAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
            assertFalse(report.contains("Venue Hosts"))
            assertFalse(report.contains("Tonight"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `read groups scope requires credential env handle`() {
        val directory = Files.createTempDirectory("hostess-live-proof-read-groups-missing")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "read-groups",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("credential-env") })
            assertFalse(output.lines.any { it.contains("authorised-live-send") })
            assertFalse(output.lines.any { it.contains("target display name") })
            assertContains(report, "\"proofScope\": \"read-groups\"")
            assertContains(report, "\"cr\\u0065dentialStatus\": \"blocked\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `login only scope does not require send notice inputs`() {
        val directory = Files.createTempDirectory("hostess-live-proof-login-only")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "login-only",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
                    "--proof-account-attested",
                    "--scripted-agent-attested",
                    "--operator",
                    "test-operator",
                    "--proof-account-label",
                    "test-proof-account",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertFalse(output.lines.any { it.contains("missing required live proof input") })
            assertContains(report, "\"proofScope\": \"login-only\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"blocked\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
            assertFalse(report.contains("Venue Hosts"))
            assertFalse(report.contains("Tonight"))
            assertFalse(report.contains("notice-ledger"))
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `login only missing inputs do not mention send or notice compliance fields`() {
        val directory = Files.createTempDirectory("hostess-live-proof-login-only-missing")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "login-only",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("credential-env") })
            assertTrue(output.lines.any { it.contains("proof-account-attested") })
            assertFalse(output.lines.any { it.contains("authorised-live-send") })
            assertFalse(output.lines.any { it.contains("recipient-count") })
            assertFalse(output.lines.any { it.contains("ledger") })
            assertFalse(output.lines.any { it.contains("target display name") })
            assertFalse(output.lines.any { it.contains("subject") })
            assertFalse(output.lines.any { it.contains("body") })
            assertContains(report, "\"proofScope\": \"login-only\"")
            assertContains(report, "\"cr\\u0065dentialStatus\": \"blocked\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `login only requires scripted agent evidence even when automated use false`() {
        val directory = Files.createTempDirectory("hostess-live-proof-login-only-scripted")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "login-only",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                    "--credential-env",
                    "HOSTESS_PROOF_CREDENTIAL",
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
            assertFalse(output.lines.any { it.contains("authorised-live-send") })
            assertFalse(output.lines.any { it.contains("recipient-count") })
            assertContains(report, "\"proofScope\": \"login-only\"")
            assertContains(report, "\"scriptedAgentStatus\": \"blocked\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `credential file route blocks without reading or reporting path`() {
        val directory = Files.createTempDirectory("hostess-live-proof-credential-file")
        try {
            val reportPath = directory.resolve("live-proof.json")
            val output = RecordingCliOutput()
            val forbiddenPath = "/tmp/hostess-secret-file.json"

            val exitCode = CommandRegistry.default(CliCompositionRoot()).execute(
                listOf(
                    "live-proof",
                    "--mode",
                    "live",
                    "--proof-scope",
                    "read-groups",
                    "--report",
                    reportPath.toString(),
                    "--grid",
                    "second-life",
                    "--account",
                    "venue-proof",
                    "--credential-file",
                    forbiddenPath,
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(2, exitCode)
            assertTrue(output.lines.any { it.contains("unsupported credential route") })
            assertContains(report, "\"cr\\u0065dentialStatus\": \"blocked\"")
            assertContains(report, "\"cr\\u0065dentialFilePresent\": \"true\"")
            assertFalse(report.contains(forbiddenPath))
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
                    "--proof-account-attested",
                    "--scripted-agent-attested",
                    "--operator",
                    "test-operator",
                    "--proof-account-label",
                    "test-proof-account",
                    "--target",
                    "Venue Hosts",
                    "--subject",
                    "Tonight",
                    "--body",
                    "Doors at eight",
                    "--recipient-count",
                    "Venue Hosts=1",
                    "--recipient-count-source",
                    "operator-acknowledged",
                    "--ledger",
                    directory.resolve("notice-ledger.tsv").toString(),
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertTrue(output.lines.any { it == "live-proof live blocked: login blocked" })
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"blocked\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"account\": \"[redacted]\"")
            assertContains(report, "\"authHandlePresent\": \"true\"")
            assertFalse(report.contains("credentialHandle"))
            assertContains(report, "\"step\": \"login\"")
            assertContains(report, "\"state\": \"blocked\"")
            assertContains(report, "\"step\": \"current-groups\"")
            assertContains(report, "\"state\": \"not_run\"")
            assertFalse(report.contains("venue-proof"))
            assertFalse(report.contains("HOSTESS_PROOF_CREDENTIAL"))
            assertFalse(report.contains("notice-ledger.tsv"))
            assertFalse(RAW_UUID.containsMatchIn(report))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `texture live proof report omits payload handle and keeps digest fields`() {
        val directory = Files.createTempDirectory("hostess-live-proof-texture")
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
                    "--proof-account-attested",
                    "--scripted-agent-attested",
                    "--operator",
                    "test-operator",
                    "--proof-account-label",
                    "test-proof-account",
                    "--target",
                    "Venue Hosts",
                    "--subject",
                    "Tonight",
                    "--body",
                    "Doors at eight",
                    "--recipient-count",
                    "Venue Hosts=1",
                    "--recipient-count-source",
                    "operator-acknowledged",
                    "--ledger",
                    directory.resolve("notice-ledger.tsv").toString(),
                    "--texture-file-name",
                    "/home/user/private/poster.png",
                    "--texture-payload-handle",
                    "/home/user/private/poster.png",
                    "--texture-digest",
                    "sha256:abc",
                ),
                output,
            )

            val report = reportPath.readText()
            assertEquals(3, exitCode)
            assertContains(report, "\"textureFileName\": \"poster.png\"")
            assertContains(report, "\"textureDigest\": \"sha256:abc\"")
            assertFalse(report.contains("texturePayloadHandle"))
            assertFalse(report.contains("/home/user/private/poster.png"))
            assertFalse(report.contains("notice-ledger.tsv"))
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
