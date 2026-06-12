package org.gem.tools.cli

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.SessionId
import org.gem.core.ports.AvatarReadinessProofStatus
import org.gem.core.ports.AvatarReadinessResult
import org.gem.tools.cli.composition.CliCompositionRoot

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
                "--attachment-kind",
                "landmark",
                "--attachment-id",
                "welcome-area",
            ),
            output,
        )

        assertEquals(0, exitCode)
        assertTrue(output.lines.any { it == "send-notice fake attempted=1" })
        assertTrue(output.lines.any { it == "Venue Hosts: sent" })
    }

    @Test
    fun `live proof validates credential handle before live execution`() {
        val directory = Files.createTempDirectory("gem-live-proof-composition")
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

    @Test
    fun `fake runtime includes avatar readiness service without enabling fake live proof`() {
        val runtime = CliCompositionRoot().runtime(CommandMode.FAKE)

        val result = assertIs<AvatarReadinessResult.Success>(
            runtime.avatarReadinessService.ensureReady(session()),
        )

        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.avatarReadinessStatus)
    }

    @Test
    fun `live runtime includes avatar readiness service through protocol composition`() {
        val runtime = CliCompositionRoot().runtime(CommandMode.LIVE)

        val result = assertIs<AvatarReadinessResult.Failure>(
            runtime.avatarReadinessService.ensureReady(session()),
        )

        assertEquals(AvatarReadinessProofStatus.RUNTIME_GAP, result.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, result.proof.simulatorPresenceStatus)
    }

    private fun session(): GemSession = GemSession(
        sessionId = SessionId("composition-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )
}
