package org.hostess.protocol.libomv.mapping

import java.util.Locale
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.InventoryItemId

internal data class LibomvAttachmentSnapshot(
    val itemId: String,
    val ownerId: String,
    val kind: AttachmentKind,
)

internal sealed interface LibomvAttachmentMappingResult {
    data class Success(val attachment: AttachmentRef) : LibomvAttachmentMappingResult
    data object Failure : LibomvAttachmentMappingResult
}

internal object LibomvAttachmentMapping {
    fun attachmentRef(
        snapshot: LibomvAttachmentSnapshot,
        expectedKind: AttachmentKind,
    ): LibomvAttachmentMappingResult {
        if (snapshot.itemId.isBlank() || snapshot.ownerId.isBlank() || snapshot.kind != expectedKind) {
            return LibomvAttachmentMappingResult.Failure
        }
        return LibomvAttachmentMappingResult.Success(
            AttachmentRef(
                attachmentId = InventoryItemId(snapshot.itemId),
                ownerId = AttachmentOwnerId(snapshot.ownerId),
                kind = snapshot.kind,
            ),
        )
    }

    fun landmarkAssetBytes(request: CreateLandmarkAttachment): ByteArray = String.format(
        Locale.US,
        "Landmark version 2\nregion_id %s\nlocal_pos %f %f %f\n",
        request.regionId,
        request.localPosition.x,
        request.localPosition.y,
        request.localPosition.z,
    ).encodeToByteArray()
}
