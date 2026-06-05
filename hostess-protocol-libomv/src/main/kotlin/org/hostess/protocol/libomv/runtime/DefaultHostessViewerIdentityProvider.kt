package org.hostess.protocol.libomv.runtime

import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest

object DefaultHostessViewerIdentityProvider : HostessViewerIdentityProvider {
    override fun resolve(): HostessViewerIdentity =
        resolve(System::getProperty, ::hardwareAddressCandidates)

    internal fun resolve(
        systemProperty: (String) -> String?,
        hardwareAddresses: () -> List<Pair<String, ByteArray>>,
    ): HostessViewerIdentity {
        val hardwareBytes = hardwareAddresses()
            .sortedBy { it.first }
            .firstOrNull()
            ?.second
            ?: throw HostessHostIdentityUnavailableException()

        return HostessViewerIdentity(
            channel = CHANNEL,
            version = VERSION,
            author = AUTHOR,
            platform = platformIdentity(systemProperty),
            host = HostessHostIdentity(
                mac = md5Hex(hardwareBytes),
                id0 = md5Hex(ID0_PREFIX, hardwareBytes),
                hostId = md5Hex(HOST_ID_PREFIX, hardwareBytes),
            ),
        )
    }

    private fun hardwareAddressCandidates(): List<Pair<String, ByteArray>> {
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
                    networkInterface.name to hardwareAddress
                } else {
                    null
                }
            } catch (ex: SocketException) {
                null
            }
        }
    }

    private fun platformIdentity(systemProperty: (String) -> String?): HostessPlatformIdentity {
        val osName = systemProperty("os.name").orEmpty()
        val osVersion = systemProperty("os.version").orEmpty()
        val osArch = systemProperty("os.arch").orEmpty()
        val runtimeName = systemProperty("java.runtime.name").orEmpty()
        val runtimeVersion = systemProperty("java.runtime.version").orEmpty()
        return HostessPlatformIdentity(
            platform = normalizedPlatform(osName),
            platformVersion = osVersion,
            platformString = listOf(osName, osVersion, osArch, runtimeName, runtimeVersion).joinToString(" "),
        )
    }

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

    private fun md5Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes).toHex()

    private fun md5Hex(prefix: ByteArray, bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(prefix)
        digest.update(bytes)
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private class HostessHostIdentityUnavailableException : IllegalStateException("host identity unavailable")

    private const val CHANNEL = "Hostess"
    private const val VERSION = "0.1.0.0"
    private const val AUTHOR = "Hostess"
    private val ID0_PREFIX = "Hostess:id0:v1".encodeToByteArray()
    private val HOST_ID_PREFIX = "Hostess:host_id:v1".encodeToByteArray()
}
