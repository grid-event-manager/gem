package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMapping
import org.hostess.protocol.libomv.mapping.LibomvAttachmentMappingResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot

class ProtocolInventoryRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val inventorySource: InventoryRuntimeSource = InventoryRuntimeSource.unavailable(),
    private val payloadSource: AttachmentPayloadSource = AttachmentPayloadSource.unavailable(),
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

    fun createLandmarkAttachment(
        session: HostessSession,
        request: CreateLandmarkAttachment,
    ): AttachmentResolutionResult {
        val bindingFailure = sessionFailure(session, CoreFailureReason.ATTACHMENT_CREATE_FAILED)
        if (bindingFailure != null) {
            return bindingFailure
        }
        return mapResult(
            result = inventorySource.createLandmarkAttachment(
                session = session,
                request = request,
                assetBytes = LibomvAttachmentMapping.landmarkAssetBytes(request),
            ),
            expectedKind = AttachmentKind.LANDMARK,
            mappingFailureReason = CoreFailureReason.ATTACHMENT_CREATE_FAILED,
            mappingFailureMessage = "landmark attachment invalid",
        )
    }

    fun uploadTextureAttachment(
        session: HostessSession,
        request: UploadTextureAttachment,
    ): AttachmentResolutionResult {
        val bindingFailure = sessionFailure(session, CoreFailureReason.ATTACHMENT_UPLOAD_FAILED)
        if (bindingFailure != null) {
            return bindingFailure
        }
        return when (val payload = payloadSource.open(request.payloadHandle, request.contentDigest)) {
            is AttachmentPayloadResult.Failed -> AttachmentResolutionResult.Failed(payload.failure)
            is AttachmentPayloadResult.Resolved -> uploadTexturePayload(session, request, payload.bytes)
        }
    }

    private fun uploadTexturePayload(
        session: HostessSession,
        request: UploadTextureAttachment,
        bytes: ByteArray,
    ): AttachmentResolutionResult =
        when (val result = inventorySource.beginTextureUpload(session, request, bytes)) {
            is InventoryUploadResult.Complete -> mapSnapshot(
                snapshot = result.snapshot,
                expectedKind = AttachmentKind.TEXTURE,
                mappingFailureReason = CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                mappingFailureMessage = "texture attachment invalid",
            )
            is InventoryUploadResult.Continue -> {
                if (result.nextEndpoint.isBlank()) {
                    failure(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, "texture upload endpoint unavailable")
                } else {
                    mapResult(
                        result = inventorySource.completeTextureUpload(session, result.nextEndpoint, bytes),
                        expectedKind = AttachmentKind.TEXTURE,
                        mappingFailureReason = CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                        mappingFailureMessage = "texture attachment invalid",
                    )
                }
            }
            is InventoryUploadResult.Failed -> failure(result.reason, result.message)
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

    fun createLandmarkAttachment(
        session: HostessSession,
        request: CreateLandmarkAttachment,
        assetBytes: ByteArray,
    ): InventoryRuntimeResult

    fun beginTextureUpload(
        session: HostessSession,
        request: UploadTextureAttachment,
        bytes: ByteArray,
    ): InventoryUploadResult

    fun completeTextureUpload(
        session: HostessSession,
        nextEndpoint: String,
        bytes: ByteArray,
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

            override fun createLandmarkAttachment(
                session: HostessSession,
                request: CreateLandmarkAttachment,
                assetBytes: ByteArray,
            ): InventoryRuntimeResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_CREATE_FAILED,
                "attachment runtime unavailable",
            )

            override fun beginTextureUpload(
                session: HostessSession,
                request: UploadTextureAttachment,
                bytes: ByteArray,
            ): InventoryUploadResult = InventoryUploadResult.Failed(
                CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                "attachment runtime unavailable",
            )

            override fun completeTextureUpload(
                session: HostessSession,
                nextEndpoint: String,
                bytes: ByteArray,
            ): InventoryRuntimeResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                "attachment runtime unavailable",
            )
        }
    }
}

internal sealed interface InventoryRuntimeResult {
    data class Success(val snapshot: LibomvAttachmentSnapshot) : InventoryRuntimeResult
    data class Failed(val reason: CoreFailureReason, val message: String) : InventoryRuntimeResult
}

internal sealed interface InventoryUploadResult {
    data class Complete(val snapshot: LibomvAttachmentSnapshot) : InventoryUploadResult
    data class Continue(val nextEndpoint: String) : InventoryUploadResult
    data class Failed(val reason: CoreFailureReason, val message: String) : InventoryUploadResult
}
