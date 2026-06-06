package org.hostess.protocol.libomv.runtime

import java.net.NetworkInterface
import java.net.SocketException

internal object DefaultHostessHardwareAddressSource : HostessHardwareAddressSource {
    override fun candidates(): List<HostessHardwareAddress> {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (ex: SocketException) {
            emptyList()
        }
        return interfaces.mapNotNull { networkInterface ->
            try {
                val hardwareAddress = networkInterface.hardwareAddress?.takeIf(ByteArray::isNotEmpty)
                if (
                    networkInterface.isUp &&
                    !networkInterface.isLoopback &&
                    !networkInterface.isVirtual &&
                    hardwareAddress != null
                ) {
                    HostessHardwareAddress(networkInterface.name, hardwareAddress.copyOf())
                } else {
                    null
                }
            } catch (ex: SocketException) {
                null
            }
        }
    }
}
