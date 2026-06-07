package org.hostess.protocol.libomv.mapping

import org.hostess.protocol.libomv.llsd.LlsdValue
import org.hostess.protocol.libomv.llsd.asLong

internal object LibomvInventoryPermissionMapping {
    fun copyable(permissions: LlsdValue?): Boolean? {
        val fields = (permissions as? LlsdValue.MapValue)?.values ?: return null
        val ownerMask = fields[OWNER_MASK]?.asLong()
            ?.takeIf { it >= 0 }
            ?: return null
        return ownerMask and COPY_PERMISSION_MASK != 0L
    }

    private const val OWNER_MASK = "owner_mask"
    private const val COPY_PERMISSION_MASK = 1L shl 15
}
