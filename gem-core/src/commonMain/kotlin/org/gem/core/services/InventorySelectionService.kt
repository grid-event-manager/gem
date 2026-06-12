package org.gem.core.services

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.InventoryAttachmentSelectionResult
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemDisplayName
import org.gem.core.domain.InventoryItemKind

class InventorySelectionService {
    fun selectExistingAttachment(
        items: Iterable<InventoryItemDescriptor>,
        displayName: InventoryItemDisplayName,
        kind: InventoryItemKind = InventoryItemKind.LANDMARK,
        requireCopyable: Boolean = true,
    ): InventoryAttachmentSelectionResult {
        val matches = items.filter { it.displayName == displayName }
        if (matches.isEmpty()) {
            return InventoryAttachmentSelectionResult.NoSuchItem(displayName)
        }

        val requiredKind = selectableKind(kind)
            ?: return InventoryAttachmentSelectionResult.WrongKind(displayName, matches, kind)
        val kindMatches = matches.filter { it.kind == requiredKind }
        if (kindMatches.isEmpty()) {
            return InventoryAttachmentSelectionResult.WrongKind(displayName, matches, requiredKind)
        }
        if (kindMatches.size > 1) {
            return InventoryAttachmentSelectionResult.AmbiguousDisplayName(displayName, kindMatches)
        }

        val descriptor = kindMatches.single()
        if (requireCopyable) {
            when (descriptor.copyable) {
                false -> return InventoryAttachmentSelectionResult.NoCopy(descriptor)
                null -> return InventoryAttachmentSelectionResult.UnknownCopyability(descriptor)
                true -> Unit
            }
        }

        return InventoryAttachmentSelectionResult.Selected(
            request = ExistingInventoryAttachment(attachmentKind(requiredKind), descriptor.itemId),
            descriptor = descriptor,
            attachmentRef = descriptor.ownerId?.let { ownerId ->
                AttachmentRef(
                    attachmentId = descriptor.itemId,
                    ownerId = ownerId,
                    kind = attachmentKind(requiredKind),
                )
            },
        )
    }

    fun selectExistingAttachmentByDisplayName(
        items: Iterable<InventoryItemDescriptor>,
        displayName: String,
        kind: InventoryItemKind = InventoryItemKind.LANDMARK,
        requireCopyable: Boolean = true,
    ): InventoryAttachmentSelectionResult =
        selectExistingAttachment(items, InventoryItemDisplayName(displayName), kind, requireCopyable)

    private fun selectableKind(kind: InventoryItemKind): InventoryItemKind? =
        when (kind) {
            InventoryItemKind.LANDMARK -> InventoryItemKind.LANDMARK
            InventoryItemKind.TEXTURE -> InventoryItemKind.TEXTURE
            InventoryItemKind.NOTECARD -> null
        }

    private fun attachmentKind(kind: InventoryItemKind): AttachmentKind =
        when (kind) {
            InventoryItemKind.LANDMARK -> AttachmentKind.LANDMARK
            InventoryItemKind.TEXTURE -> AttachmentKind.TEXTURE
            InventoryItemKind.NOTECARD -> error("Notecard inventory items are not notice attachments.")
        }
}
