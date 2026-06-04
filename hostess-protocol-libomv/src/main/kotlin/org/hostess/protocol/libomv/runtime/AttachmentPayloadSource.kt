package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.CoreFailure

interface AttachmentPayloadSource {
    fun open(handle: AttachmentPayloadHandle, expectedDigest: String): AttachmentPayloadResult
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
