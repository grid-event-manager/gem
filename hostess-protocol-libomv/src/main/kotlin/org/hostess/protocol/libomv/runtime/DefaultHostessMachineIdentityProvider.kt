package org.hostess.protocol.libomv.runtime

object DefaultHostessMachineIdentityProvider : HostessMachineIdentityProvider {
    override fun resolve(): HostessMachineIdentity =
        resolve(DefaultHostessHardwareAddressSource)

    internal fun resolve(
        hardwareAddressSource: HostessHardwareAddressSource,
    ): HostessMachineIdentity {
        val hardwareBytes = hardwareAddressSource.candidates()
            .filter { it.bytes.size >= MIN_HARDWARE_ADDRESS_LENGTH }
            .sortedBy { it.interfaceName }
            .firstOrNull()
            ?.bytes
            ?: throw HostessMachineIdentityUnavailableException()
        return HostessMachineIdentity.fromHardwareBytes(hardwareBytes)
    }

    private class HostessMachineIdentityUnavailableException : IllegalStateException("host identity unavailable")

    private const val MIN_HARDWARE_ADDRESS_LENGTH = 6
}
