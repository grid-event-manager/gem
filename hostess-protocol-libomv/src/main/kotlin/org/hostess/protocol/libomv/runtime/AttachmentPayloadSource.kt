package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason

fun interface AttachmentPayloadSource {
    fun open(handle: AttachmentPayloadHandle, expectedDigest: String): AttachmentPayloadResult

    companion object {
        fun unavailable(): AttachmentPayloadSource = AttachmentPayloadSource { _, _ ->
            AttachmentPayloadResult.Failed(
                CoreFailure(
                    CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                    redactedMessage = "texture payload unavailable",
                ),
            )
        }
    }
}

sealed interface AttachmentPayloadResult {
    data class Resolved(
        val bytes: ByteArray,
        val digest: String,
    ) : AttachmentPayloadResult {
        init {
            require(digest.isNotBlank()) { "Attachment payload digest cannot be blank." }
        }
    }

    data class Failed(val failure: CoreFailure) : AttachmentPayloadResult
}
