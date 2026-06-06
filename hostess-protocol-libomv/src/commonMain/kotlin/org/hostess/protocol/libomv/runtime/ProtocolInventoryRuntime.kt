package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMapping
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMappingResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot

class ProtocolInventoryRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val inventorySource: InventoryRuntimeSource = InventoryRuntimeSource.unavailable(),
) {
    fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult {
        val bindingFailure = sessionFailure(session, CoreFailureReason.ATTACHMENT_NOT_FOUND)
        if (bindingFailure != null) {
            return bindingFailure
        }
        return mapResult(
            result = inventorySource.resolveExistingAttachment(session, request),
            expectedKind = request.kind,
            mappingFailureReason = CoreFailureReason.ATTACHMENT_NOT_FOUND,
            mappingFailureMessage = "attachment kind mismatch",
        )
    }

    private fun sessionFailure(
        session: HostessSession,
        reason: CoreFailureReason,
    ): AttachmentResolutionResult.Failed? =
        clientSession.requireSession(session)
            ?.let { AttachmentResolutionResult.Failed(it.copy(reason = reason)) }

    private fun mapResult(
        result: InventoryRuntimeResult,
        expectedKind: AttachmentKind,
        mappingFailureReason: CoreFailureReason,
        mappingFailureMessage: String,
    ): AttachmentResolutionResult =
        when (result) {
            is InventoryRuntimeResult.Failed -> failure(result.reason, result.message)
            is InventoryRuntimeResult.Success -> mapSnapshot(
                result.snapshot,
                expectedKind,
                mappingFailureReason,
                mappingFailureMessage,
            )
        }

    private fun mapSnapshot(
        snapshot: LibomvAttachmentSnapshot,
        expectedKind: AttachmentKind,
        mappingFailureReason: CoreFailureReason,
        mappingFailureMessage: String,
    ): AttachmentResolutionResult =
        when (val mapped = LibomvAttachmentMapping.attachmentRef(snapshot, expectedKind)) {
            LibomvAttachmentMappingResult.Failure -> failure(mappingFailureReason, mappingFailureMessage)
            is LibomvAttachmentMappingResult.Success -> AttachmentResolutionResult.Resolved(mapped.attachment)
        }

    private fun failure(reason: CoreFailureReason, message: String): AttachmentResolutionResult.Failed =
        AttachmentResolutionResult.Failed(CoreFailure(reason, redactedMessage = message))
}

internal interface InventoryRuntimeSource {
    fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): InventoryRuntimeResult

    companion object {
        fun unavailable(): InventoryRuntimeSource = object : InventoryRuntimeSource {
            override fun resolveExistingAttachment(
                session: HostessSession,
                request: ExistingInventoryAttachment,
            ): InventoryRuntimeResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_NOT_FOUND,
                "attachment runtime unavailable",
            )
        }
    }
}

internal sealed interface InventoryRuntimeResult {
    data class Success(val snapshot: LibomvAttachmentSnapshot) : InventoryRuntimeResult
    data class Failed(val reason: CoreFailureReason, val message: String) : InventoryRuntimeResult
}
