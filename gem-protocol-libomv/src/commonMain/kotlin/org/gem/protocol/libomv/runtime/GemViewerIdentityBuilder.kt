package org.gem.protocol.libomv.runtime

internal data class GemSystemIdentity(
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val runtimeName: String,
    val runtimeVersion: String,
)

internal class GemViewerIdentityBuilder(
    private val digestPort: Md5DigestPort,
) {
    fun build(
        systemIdentity: GemSystemIdentity,
        hardwareCandidates: List<GemHardwareAddress>,
    ): GemViewerIdentity {
        val hardwareBytes = hardwareCandidates
            .sortedBy { it.interfaceName }
            .firstOrNull()
            ?.bytes
            ?: throw GemHostIdentityUnavailableException()

        return GemViewerIdentity(
            channel = CHANNEL,
            version = VERSION,
            author = AUTHOR,
            platform = platformIdentity(systemIdentity),
            host = GemHostIdentity(
                mac = digestPort.md5Hex(hardwareBytes),
                id0 = digestPort.md5Hex(ID0_PREFIX, hardwareBytes),
                hostId = digestPort.md5Hex(HOST_ID_PREFIX, hardwareBytes),
            ),
        )
    }

    private fun platformIdentity(systemIdentity: GemSystemIdentity): GemPlatformIdentity =
        GemPlatformIdentity(
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

    private class GemHostIdentityUnavailableException : IllegalStateException("host identity unavailable")

    private companion object {
        const val CHANNEL = "GEM"
        const val VERSION = "0.1.0.0"
        const val AUTHOR = "GEM"
        val ID0_PREFIX = "GEM:id0:v1".encodeToByteArray()
        val HOST_ID_PREFIX = "GEM:host_id:v1".encodeToByteArray()
    }
}
