package org.gem.protocol.libomv.runtime

internal fun interface GemHardwareAddressSource {
    fun candidates(): List<GemHardwareAddress>
}

internal class GemHardwareAddress(
    val interfaceName: String,
    val bytes: ByteArray,
)
