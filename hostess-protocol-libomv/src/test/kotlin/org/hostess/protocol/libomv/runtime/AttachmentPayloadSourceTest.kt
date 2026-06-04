package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AttachmentPayloadSourceTest {
    @Test
    fun `fake payload source resolves bytes by opaque handle`() {
        val source = FakeAttachmentPayloadSource(
            mapOf("texture-handle" to FakeAttachmentPayload("sha256:abc", byteArrayOf(1, 2, 3))),
        )

        val result = assertIs<AttachmentPayloadResult.Resolved>(
            source.open(AttachmentPayloadHandle("texture-handle"), "sha256:abc"),
        )

        assertEquals("sha256:abc", result.digest)
        assertContentEquals(byteArrayOf(1, 2, 3), result.bytes)
    }

    @Test
    fun `digest mismatch fails before upload`() {
        val source = FakeAttachmentPayloadSource(
            mapOf("texture-handle" to FakeAttachmentPayload("sha256:abc", byteArrayOf(1, 2, 3))),
        )

        val result = assertIs<AttachmentPayloadResult.Failed>(
            source.open(AttachmentPayloadHandle("texture-handle"), "sha256:expected"),
        )

        assertEquals(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, result.failure.reason)
        assertEquals("texture payload digest mismatch", result.failure.redactedMessage)
    }

    @Test
    fun `missing handle fails without echoing handle value`() {
        val source = FakeAttachmentPayloadSource(emptyMap())

        val result = assertIs<AttachmentPayloadResult.Failed>(
            source.open(AttachmentPayloadHandle("secret-local-path"), "sha256:expected"),
        )

        assertEquals(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, result.failure.reason)
        assertEquals("texture payload unavailable", result.failure.redactedMessage)
    }

    private data class FakeAttachmentPayload(
        val digest: String,
        val bytes: ByteArray,
    )

    private class FakeAttachmentPayloadSource(
        private val payloads: Map<String, FakeAttachmentPayload>,
    ) : AttachmentPayloadSource {
        override fun open(handle: AttachmentPayloadHandle, expectedDigest: String): AttachmentPayloadResult {
            val payload = payloads[handle.value]
                ?: return failure("texture payload unavailable")
            if (payload.digest != expectedDigest) {
                return failure("texture payload digest mismatch")
            }
            return AttachmentPayloadResult.Resolved(payload.bytes, payload.digest)
        }

        private fun failure(message: String): AttachmentPayloadResult.Failed =
            AttachmentPayloadResult.Failed(
                CoreFailure(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, redactedMessage = message),
            )
    }
}
