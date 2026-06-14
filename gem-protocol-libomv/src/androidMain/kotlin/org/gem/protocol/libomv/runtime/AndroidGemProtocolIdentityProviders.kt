package org.gem.protocol.libomv.runtime

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

internal object AndroidGemProtocolIdentityProviders {
    fun create(context: Context): GemProtocolIdentityProviders {
        val hardwareIdentityBytes = AndroidStableDeviceIdentity.bytesFor(context)
        return GemProtocolIdentityProviders(
            viewerIdentityProvider = GemViewerIdentityProvider {
                GemProtocolIdentityFactory.viewerIdentity(
                    systemProperty = System::getProperty,
                    hardwareIdentityName = AndroidStableDeviceIdentity.HARDWARE_IDENTITY_NAME,
                    hardwareIdentityBytes = hardwareIdentityBytes,
                )
            },
            machineIdentityProvider = GemMachineIdentityProvider {
                GemProtocolIdentityFactory.machineIdentity(hardwareIdentityBytes)
            },
        )
    }
}

internal data class GemProtocolIdentityProviders(
    val viewerIdentityProvider: GemViewerIdentityProvider,
    val machineIdentityProvider: GemMachineIdentityProvider,
)

internal object AndroidStableDeviceIdentity {
    const val HARDWARE_IDENTITY_NAME: String = "android-device"

    fun bytesFor(context: Context): ByteArray =
        bytesFromSeed(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeUnless(::isKnownBadAndroidId)
                ?: listOf(
                    Build.MANUFACTURER,
                    Build.BRAND,
                    Build.MODEL,
                    Build.DEVICE,
                    Build.FINGERPRINT,
                ).joinToString(separator = "|"),
        )

    internal fun bytesFromSeed(seed: String): ByteArray {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest("GEM Android device identity v1:$seed".encodeToByteArray())
        return digest.copyOfRange(0, HARDWARE_IDENTITY_BYTE_COUNT).also { bytes ->
            bytes[0] = ((bytes[0].toInt() and 0xff) or LOCALLY_ADMINISTERED_BIT)
                .and(UNICAST_MASK)
                .toByte()
        }
    }

    private fun isKnownBadAndroidId(value: String): Boolean =
        value.isBlank() || value == "9774d56d682e549c"

    private const val HARDWARE_IDENTITY_BYTE_COUNT = 6
    private const val LOCALLY_ADMINISTERED_BIT = 0x02
    private const val UNICAST_MASK = 0xfe
}
