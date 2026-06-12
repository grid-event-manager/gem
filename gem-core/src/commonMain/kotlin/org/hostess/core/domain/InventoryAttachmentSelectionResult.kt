package org.hostess.core.domain

sealed interface InventoryAttachmentSelectionResult {
    data class Selected(
        val request: ExistingInventoryAttachment,
        val descriptor: InventoryItemDescriptor,
        val attachmentRef: AttachmentRef?,
    ) : InventoryAttachmentSelectionResult

    data class NoSuchItem(
        val displayName: InventoryItemDisplayName,
    ) : InventoryAttachmentSelectionResult

    data class AmbiguousDisplayName(
        val displayName: InventoryItemDisplayName,
        val matches: List<InventoryItemDescriptor>,
    ) : InventoryAttachmentSelectionResult

    data class WrongKind(
        val displayName: InventoryItemDisplayName,
        val matches: List<InventoryItemDescriptor>,
        val requiredKind: InventoryItemKind,
    ) : InventoryAttachmentSelectionResult

    data class NoCopy(
        val descriptor: InventoryItemDescriptor,
    ) : InventoryAttachmentSelectionResult

    data class UnknownCopyability(
        val descriptor: InventoryItemDescriptor,
    ) : InventoryAttachmentSelectionResult
}
