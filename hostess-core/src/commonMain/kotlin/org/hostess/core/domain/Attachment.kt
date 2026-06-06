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

data class AttachmentRef(
    val attachmentId: InventoryItemId,
    val ownerId: AttachmentOwnerId,
    val kind: AttachmentKind,
)
