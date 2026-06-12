package org.gem.protocol.libomv.runtime

object DefaultHostessViewerIdentityProvider : HostessViewerIdentityProvider {
    override fun resolve(): HostessViewerIdentity =
        resolve(
            systemProperty = System::getProperty,
            hardwareAddressSource = DefaultHostessHardwareAddressSource,
        )

    internal fun resolve(
        systemProperty: (String) -> String?,
        hardwareAddresses: () -> List<Pair<String, ByteArray>>,
    ): HostessViewerIdentity =
        resolve(
            systemProperty = systemProperty,
            hardwareAddressSource = HostessHardwareAddressSource {
                hardwareAddresses().map { (name, bytes) -> HostessHardwareAddress(name, bytes.copyOf()) }
            },
        )

    internal fun resolve(
        systemProperty: (String) -> String?,
        hardwareAddressSource: HostessHardwareAddressSource,
        digestPort: Md5DigestPort = JvmMd5DigestPort,
    ): HostessViewerIdentity =
        HostessViewerIdentityBuilder(digestPort).build(
            systemIdentity = HostessSystemIdentity(
                osName = systemProperty("os.name").orEmpty(),
                osVersion = systemProperty("os.version").orEmpty(),
                osArch = systemProperty("os.arch").orEmpty(),
                runtimeName = systemProperty("java.runtime.name").orEmpty(),
                runtimeVersion = systemProperty("java.runtime.version").orEmpty(),
            ),
            hardwareCandidates = hardwareAddressSource.candidates(),
        )
}
