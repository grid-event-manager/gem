package org.gem.protocol.libomv.transport

internal enum class CapabilityName(val wireName: String) {
    EVENT_QUEUE_GET("EventQueueGet"),
    FETCH_INVENTORY_DESCENDENTS2("FetchInventoryDescendents2"),
    UPDATE_AVATAR_APPEARANCE("UpdateAvatarAppearance"),
}
