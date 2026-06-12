package org.gem.core.domain

@JvmInline
value class GroupId(val value: String) {
    init {
        require(value.isNotBlank()) { "GroupId cannot be blank." }
    }
}

@JvmInline
value class InventoryItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "InventoryItemId cannot be blank." }
    }
}

@JvmInline
value class InventoryItemDisplayName(val value: String) {
    init {
        require(value.isNotBlank()) { "InventoryItemDisplayName cannot be blank." }
    }
}

@JvmInline
value class InventoryFolderId(val value: String) {
    init {
        require(value.isNotBlank()) { "InventoryFolderId cannot be blank." }
    }
}

@JvmInline
value class InventoryAssetId(val value: String) {
    init {
        require(value.isNotBlank()) { "InventoryAssetId cannot be blank." }
    }
}

@JvmInline
value class AttachmentOwnerId(val value: String) {
    init {
        require(value.isNotBlank()) { "AttachmentOwnerId cannot be blank." }
    }
}

@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank." }
    }
}

@JvmInline
value class AccountLabel(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountLabel cannot be blank." }
    }
}

@JvmInline
value class GroupDisplayName(val value: String) {
    init {
        require(value.isNotBlank()) { "GroupDisplayName cannot be blank." }
    }
}
