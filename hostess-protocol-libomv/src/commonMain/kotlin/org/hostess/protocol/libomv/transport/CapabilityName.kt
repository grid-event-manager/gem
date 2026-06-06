package org.hostess.protocol.libomv.transport

internal enum class CapabilityName(val wireName: String) {
    EVENT_QUEUE_GET("EventQueueGet"),
    FETCH_INVENTORY2("FetchInventory2"),
    FETCH_INVENTORY_DESCENDENTS2("FetchInventoryDescendents2"),
}
