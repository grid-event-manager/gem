package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostessViewerIdentityProviderTest {
    @Test
    fun `default provider resolves truthful Hostess identity from deterministic host inputs`() {
        val identity = DefaultHostessViewerIdentityProvider.resolve(
            systemProperty = mapOf(
                "os.name" to "Linux",
                "os.version" to "6.8.0",
                "os.arch" to "amd64",
                "java.runtime.name" to "OpenJDK Runtime Environment",
                "java.runtime.version" to "17.0.11",
            )::get,
            hardwareAddresses = {
                listOf(
                    "wlan0" to byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x02),
                    "eth0" to byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x01),
                )
            },
        )

        assertEquals("Hostess", identity.channel)
        assertEquals("0.1.0.0", identity.version)
        assertEquals("Hostess", identity.author)
        assertEquals("Linux", identity.platform.platform)
        assertEquals("6.8.0", identity.platform.platformVersion)
        assertTrue(identity.platform.platformString.contains("Linux"))
        assertTrue(identity.platform.platformString.contains("amd64"))
        assertTrue(identity.host.mac.matches(DIGEST_PATTERN))
        assertTrue(identity.host.id0.matches(DIGEST_PATTERN))
        assertTrue(identity.host.hostId.matches(DIGEST_PATTERN))
        assertFalse(identity.platform.platformString.contains(identity.host.mac))
        assertFalse(identity.platform.platformString.contains(identity.host.id0))
        assertFalse(identity.platform.platformString.contains(identity.host.hostId))
    }

    @Test
    fun `default provider fails closed without host identity`() {
        val failure = assertFailsWith<IllegalStateException> {
            DefaultHostessViewerIdentityProvider.resolve(
                systemProperty = { "" },
                hardwareAddresses = { emptyList() },
            )
        }

        assertEquals("host identity unavailable", failure.message)
    }

    @Test
    fun `platform normalization follows supported names`() {
        assertEquals("Android", platformFor("Android"))
        assertEquals("Linux", platformFor("GNU/Linux"))
        assertEquals("Mac", platformFor("Darwin"))
        assertEquals("Win", platformFor("Windows 11"))
        assertEquals("Plan9", platformFor("Plan9"))
        assertEquals("Unknown", platformFor(""))
    }

    @Test
    fun `host identity rejects non-redacted values`() {
        assertFailsWith<IllegalArgumentException> {
            HostessHostIdentity(
                mac = "raw-mac-address",
                id0 = "00000000000000000000000000000000",
                hostId = "00000000000000000000000000000000",
            )
        }
    }

    private fun platformFor(osName: String): String =
        DefaultHostessViewerIdentityProvider.resolve(
            systemProperty = mapOf(
                "os.name" to osName,
                "os.version" to "",
                "os.arch" to "",
                "java.runtime.name" to "",
                "java.runtime.version" to "",
            )::get,
            hardwareAddresses = { listOf("eth0" to byteArrayOf(0x01)) },
        ).platform.platform

    private companion object {
        val DIGEST_PATTERN = Regex("[0-9a-f]{32}")
    }
}
