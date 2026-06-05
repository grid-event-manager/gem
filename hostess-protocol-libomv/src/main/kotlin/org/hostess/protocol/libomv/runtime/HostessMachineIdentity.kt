package org.hostess.protocol.libomv.runtime

data class HostessMachineIdentity(
    val mac: String,
    val id0: String,
) {
    init {
        require(RAW_MAC_PATTERN.matches(mac)) { "mac must be a raw uppercase colon-separated hardware address." }
        require(RAW_MAC_PATTERN.matches(id0)) { "id0 must be a raw uppercase colon-separated hardware address." }
    }

    internal companion object {
        fun fromHardwareBytes(bytes: ByteArray): HostessMachineIdentity {
            require(bytes.size >= MIN_HARDWARE_ADDRESS_LENGTH) { "host identity unavailable" }
            val address = bytes.joinToString(":") { "%02X".format(it.toInt() and 0xff) }
            return HostessMachineIdentity(mac = address, id0 = address)
        }

        private val RAW_MAC_PATTERN = Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5,}")
        private const val MIN_HARDWARE_ADDRESS_LENGTH = 6
    }
}
