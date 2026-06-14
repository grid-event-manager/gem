package org.gem.protocol.libomv.runtime

object GemProtocolIdentityFactory {
    fun viewerIdentity(
        systemProperty: (String) -> String?,
        hardwareIdentityName: String,
        hardwareIdentityBytes: ByteArray,
    ): GemViewerIdentity =
        DefaultGemViewerIdentityProvider.resolve(
            systemProperty = systemProperty,
            hardwareAddresses = {
                listOf(hardwareIdentityName to hardwareIdentityBytes.copyOf())
            },
        )

    fun machineIdentity(hardwareIdentityBytes: ByteArray): GemMachineIdentity =
        GemMachineIdentity.fromHardwareBytes(hardwareIdentityBytes.copyOf())
}
