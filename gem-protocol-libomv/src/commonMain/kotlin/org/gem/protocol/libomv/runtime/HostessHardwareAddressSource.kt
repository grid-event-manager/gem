package org.gem.protocol.libomv.runtime

internal fun interface HostessHardwareAddressSource {
    fun candidates(): List<HostessHardwareAddress>
}

internal class HostessHardwareAddress(
    val interfaceName: String,
    val bytes: ByteArray,
)
