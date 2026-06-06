package org.hostess.protocol.libomv.mapping

import kotlin.math.round
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

    fun landmarkAssetBytes(request: CreateLandmarkAttachment): ByteArray = buildString {
        append("Landmark version 2\n")
        append("region_id ").append(request.regionId).append('\n')
        append("local_pos ")
            .append(formatCoordinate(request.localPosition.x)).append(' ')
            .append(formatCoordinate(request.localPosition.y)).append(' ')
            .append(formatCoordinate(request.localPosition.z)).append('\n')
    }.encodeToByteArray()

    private fun formatCoordinate(value: Double): String {
        val scaled = round(value * COORDINATE_SCALE).toLong()
        val sign = if (scaled < 0) "-" else ""
        val absolute = if (scaled < 0) -scaled else scaled
        val whole = absolute / COORDINATE_SCALE_INT
        val fraction = (absolute % COORDINATE_SCALE_INT).toString().padStart(6, '0')
        return "$sign$whole.$fraction"
    }

    private const val COORDINATE_SCALE: Double = 1_000_000.0
    private const val COORDINATE_SCALE_INT: Long = 1_000_000L
}
