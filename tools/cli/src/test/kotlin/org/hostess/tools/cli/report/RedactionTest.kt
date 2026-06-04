package org.hostess.tools.cli.report

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactionTest {
    @Test
    fun `redacts key based account group credential and session values`() {
        val samples = mapOf(
            "account" to "venue-proof",
            "groupId" to "12345678-1234-1234-1234-123456789abc",
            "session_id" to "session-material",
            "credentialHandle" to "HOSTESS_PROOF_CREDENTIAL",
            "existingAttachmentId" to "12345678-1234-1234-1234-123456789abc",
            "landmarkRegionId" to "12345678-1234-1234-1234-123456789abc",
            "password" to "secret",
            "retentionNote" to "keep this private",
            "attachmentPayloadHandle" to "/home/user/poster.png",
            "attachmentSource" to "/home/user/poster.png",
            "texturePayloadHandle" to "/home/user/poster.png",
        )

        assertEquals(
            samples.keys.associateWith { "[redacted]" },
            RedactedText.map(samples),
        )
    }

    @Test
    fun `redacts token URL and UUID shaped values in ordinary fields`() {
        val redacted = RedactedText.from(
            "detail",
            "Bearer token-123 seed_cap=abc upload_url=https://example.invalid/upload " +
                "12345678-1234-1234-1234-123456789abc",
        ).value

        assertEquals(
            "Bearer [redacted] seed_cap=[redacted] upload_url=[redacted] [redacted-group-id]",
            redacted,
        )
    }
}
