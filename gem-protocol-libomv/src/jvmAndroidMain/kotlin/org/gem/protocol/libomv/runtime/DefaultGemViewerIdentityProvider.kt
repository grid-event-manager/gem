package org.gem.protocol.libomv.runtime

object DefaultGemViewerIdentityProvider : GemViewerIdentityProvider {
    override fun resolve(): GemViewerIdentity =
        resolve(
            systemProperty = System::getProperty,
            hardwareAddressSource = DefaultGemHardwareAddressSource,
        )

    internal fun resolve(
        systemProperty: (String) -> String?,
        hardwareAddresses: () -> List<Pair<String, ByteArray>>,
    ): GemViewerIdentity =
        resolve(
            systemProperty = systemProperty,
            hardwareAddressSource = GemHardwareAddressSource {
                hardwareAddresses().map { (name, bytes) -> GemHardwareAddress(name, bytes.copyOf()) }
            },
        )

    internal fun resolve(
        systemProperty: (String) -> String?,
        hardwareAddressSource: GemHardwareAddressSource,
        digestPort: Md5DigestPort = JvmMd5DigestPort,
    ): GemViewerIdentity =
        GemViewerIdentityBuilder(digestPort).build(
            systemIdentity = GemSystemIdentity(
                osName = systemProperty("os.name").orEmpty(),
                osVersion = systemProperty("os.version").orEmpty(),
                osArch = systemProperty("os.arch").orEmpty(),
                runtimeName = systemProperty("java.runtime.name").orEmpty(),
                runtimeVersion = systemProperty("java.runtime.version").orEmpty(),
            ),
            hardwareCandidates = hardwareAddressSource.candidates(),
        )
}
