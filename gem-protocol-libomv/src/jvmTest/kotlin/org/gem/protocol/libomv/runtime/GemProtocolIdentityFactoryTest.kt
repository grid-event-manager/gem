package org.gem.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemProtocolIdentityFactoryTest {
    @Test
    fun `factory builds viewer and machine identities from supplied platform bytes`() {
        val bytes = byteArrayOf(0x02, 0x12, 0x34, 0x56, 0x78, 0x10)

        val viewerIdentity = GemProtocolIdentityFactory.viewerIdentity(
            systemProperty = mapOf(
                "os.name" to "Android",
                "os.version" to "16",
                "os.arch" to "aarch64",
                "java.runtime.name" to "Android Runtime",
                "java.runtime.version" to "0",
            )::get,
            hardwareIdentityName = "android-device",
            hardwareIdentityBytes = bytes,
        )
        val machineIdentity = GemProtocolIdentityFactory.machineIdentity(bytes)

        assertEquals("GEM", viewerIdentity.channel)
        assertEquals("Android", viewerIdentity.platform.platform)
        assertTrue(viewerIdentity.host.mac.matches(Regex("[0-9a-f]{32}")))
        assertEquals("02:12:34:56:78:10", machineIdentity.mac)
        assertEquals("02:12:34:56:78:10", machineIdentity.id0)
    }
}
