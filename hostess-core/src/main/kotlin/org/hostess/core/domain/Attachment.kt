package org.hostess.core.domain

enum class AttachmentKind {
    LANDMARK,
    TEXTURE,
}

sealed interface AttachmentRequest {
    val kind: AttachmentKind
}

data class ExistingInventoryAttachment(
    override val kind: AttachmentKind,
    val itemId: InventoryItemId,
) : AttachmentRequest

data class CreateLandmarkAttachment(
    val venueLabel: String,
    val regionId: String,
    val localPosition: LocalPosition,
) : AttachmentRequest {
    init {
        require(venueLabel.isNotBlank()) { "Venue label cannot be blank." }
        require(regionId.isNotBlank()) { "Region ID cannot be blank." }
    }

    override val kind: AttachmentKind = AttachmentKind.LANDMARK
}

data class UploadTextureAttachment(
    val fileName: String,
    val contentDigest: String,
) : AttachmentRequest {
    init {
        require(fileName.isNotBlank()) { "Texture file name cannot be blank." }
        require(contentDigest.isNotBlank()) { "Texture content digest cannot be blank." }
    }

    override val kind: AttachmentKind = AttachmentKind.TEXTURE
}

data class AttachmentRef(
    val attachmentId: InventoryItemId,
    val ownerId: AttachmentOwnerId,
    val kind: AttachmentKind,
)

data class LocalPosition(
    val x: Double,
    val y: Double,
    val z: Double,
)
