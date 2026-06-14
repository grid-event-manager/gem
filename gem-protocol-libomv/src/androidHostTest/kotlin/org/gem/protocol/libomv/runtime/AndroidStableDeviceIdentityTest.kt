package org.gem.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AndroidStableDeviceIdentityTest {
    @Test
    fun stableIdentityBytesAreSixByteLocallyAdministeredUnicastValues() {
        val first = AndroidStableDeviceIdentity.bytesFromSeed("android-id-one")
        val second = AndroidStableDeviceIdentity.bytesFromSeed("android-id-one")
        val different = AndroidStableDeviceIdentity.bytesFromSeed("android-id-two")

        assertEquals(6, first.size)
        assertEquals(first.toList(), second.toList())
        assertFalse(first.contentEquals(different))
        assertEquals(0x02, first[0].toInt() and 0x03)
    }
}
