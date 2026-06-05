package org.hostess.protocol.libomv.runtime

internal data class HostessSystemIdentity(
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val runtimeName: String,
    val runtimeVersion: String,
)

internal class HostessViewerIdentityBuilder(
    private val digestPort: Md5DigestPort,
) {
    fun build(
        systemIdentity: HostessSystemIdentity,
        hardwareCandidates: List<HostessHardwareAddress>,
    ): HostessViewerIdentity {
        val hardwareBytes = hardwareCandidates
            .sortedBy { it.interfaceName }
            .firstOrNull()
            ?.bytes
            ?: throw HostessHostIdentityUnavailableException()

        return HostessViewerIdentity(
            channel = CHANNEL,
            version = VERSION,
            author = AUTHOR,
            platform = platformIdentity(systemIdentity),
            host = HostessHostIdentity(
                mac = digestPort.md5Hex(hardwareBytes),
                id0 = digestPort.md5Hex(ID0_PREFIX, hardwareBytes),
                hostId = digestPort.md5Hex(HOST_ID_PREFIX, hardwareBytes),
            ),
        )
    }

    private fun platformIdentity(systemIdentity: HostessSystemIdentity): HostessPlatformIdentity =
        HostessPlatformIdentity(
            platform = normalizedPlatform(systemIdentity.osName),
            platformVersion = systemIdentity.osVersion,
            platformString = listOf(
                systemIdentity.osName,
                systemIdentity.osVersion,
                systemIdentity.osArch,
                systemIdentity.runtimeName,
                systemIdentity.runtimeVersion,
            ).joinToString(" "),
        )

    private fun normalizedPlatform(osName: String): String {
        val lower = osName.lowercase()
        return when {
            "android" in lower -> "Android"
            "linux" in lower -> "Linux"
            "mac" in lower || "darwin" in lower -> "Mac"
            "windows" in lower -> "Win"
            osName.isNotBlank() -> osName
            else -> "Unknown"
        }
    }

    private class HostessHostIdentityUnavailableException : IllegalStateException("host identity unavailable")

    private companion object {
        const val CHANNEL = "Hostess"
        const val VERSION = "0.1.0.0"
        const val AUTHOR = "Hostess"
        val ID0_PREFIX = "Hostess:id0:v1".encodeToByteArray()
        val HOST_ID_PREFIX = "Hostess:host_id:v1".encodeToByteArray()
    }
}
