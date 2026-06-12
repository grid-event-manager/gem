package org.gem.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GemMachineIdentityTest {
    @Test
    fun `default provider emits raw uppercase colon mac and id0 from selected hardware address`() {
        val identity = DefaultGemMachineIdentityProvider.resolve(
            GemHardwareAddressSource {
                listOf(
                    GemHardwareAddress("wlan0", byteArrayOf(0x08, 0x00, 0x27, 0xDC.toByte(), 0x4A, 0x9F.toByte())),
                    GemHardwareAddress("eth0", byteArrayOf(0x08, 0x00, 0x27, 0xDC.toByte(), 0x4A, 0x9E.toByte())),
                )
            },
        )

        assertEquals("08:00:27:DC:4A:9E", identity.mac)
        assertEquals("08:00:27:DC:4A:9E", identity.id0)
    }

    @Test
    fun `default provider fails closed when no hardware address is eligible`() {
        val failure = assertFailsWith<IllegalStateException> {
            DefaultGemMachineIdentityProvider.resolve(
                GemHardwareAddressSource {
                    listOf(GemHardwareAddress("lo", byteArrayOf(0x01)))
                },
            )
        }

        assertEquals("host identity unavailable", failure.message)
    }

    @Test
    fun `machine identity rejects digest-shaped or lowercase values`() {
        assertFailsWith<IllegalArgumentException> {
            GemMachineIdentity(
                mac = "00000000000000000000000000000000",
                id0 = "08:00:27:DC:4A:9E",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GemMachineIdentity(
                mac = "08:00:27:dc:4a:9e",
                id0 = "08:00:27:DC:4A:9E",
            )
        }
    }
}
