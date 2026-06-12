package org.gem.protocol.libomv.runtime

data class GemMachineIdentity(
    val mac: String,
    val id0: String,
) {
    init {
        require(RAW_MAC_PATTERN.matches(mac)) { "mac must be a raw uppercase colon-separated hardware address." }
        require(RAW_MAC_PATTERN.matches(id0)) { "id0 must be a raw uppercase colon-separated hardware address." }
    }

    internal companion object {
        fun fromHardwareBytes(bytes: ByteArray): GemMachineIdentity {
            require(bytes.size >= MIN_HARDWARE_ADDRESS_LENGTH) { "host identity unavailable" }
            val address = bytes.joinToString(":") { byte -> byte.toUpperHexByte() }
            return GemMachineIdentity(mac = address, id0 = address)
        }

        private fun Byte.toUpperHexByte(): String {
            val value = toInt() and 0xff
            return "${HEX[value / HEX_BASE]}${HEX[value % HEX_BASE]}"
        }

        private val RAW_MAC_PATTERN = Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5,}")
        private const val HEX = "0123456789ABCDEF"
        private const val HEX_BASE = 16
        private const val MIN_HARDWARE_ADDRESS_LENGTH = 6
    }
}
