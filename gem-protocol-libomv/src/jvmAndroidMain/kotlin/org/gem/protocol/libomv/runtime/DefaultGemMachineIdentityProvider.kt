package org.gem.protocol.libomv.runtime

object DefaultGemMachineIdentityProvider : GemMachineIdentityProvider {
    override fun resolve(): GemMachineIdentity =
        resolve(DefaultGemHardwareAddressSource)

    internal fun resolve(
        hardwareAddressSource: GemHardwareAddressSource,
    ): GemMachineIdentity {
        val hardwareBytes = hardwareAddressSource.candidates()
            .filter { it.bytes.size >= MIN_HARDWARE_ADDRESS_LENGTH }
            .sortedBy { it.interfaceName }
            .firstOrNull()
            ?.bytes
            ?: throw GemMachineIdentityUnavailableException()
        return GemMachineIdentity.fromHardwareBytes(hardwareBytes)
    }

    private class GemMachineIdentityUnavailableException : IllegalStateException("host identity unavailable")

    private const val MIN_HARDWARE_ADDRESS_LENGTH = 6
}
