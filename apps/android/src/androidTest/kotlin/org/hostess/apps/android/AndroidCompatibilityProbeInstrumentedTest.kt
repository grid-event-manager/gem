package org.hostess.apps.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCompatibilityProbeInstrumentedTest {
    @Test
    fun probePassesOnAttachedAndroidDevice() {
        val result = AndroidCompatibilityProbe().run()

        assertEquals("passed", result.status)
        assertTrue(result.coreCompile)
        assertTrue(result.adapterLoad)
        assertTrue(result.runtimeLoad)
        assertTrue(result.transportLoad)
        assertTrue(result.trackCClassLoad)
        assertTrue(result.trackDComplianceLoad)
        assertTrue(result.trackDsLoginPackageLoad)
        assertTrue(result.trackGGridLoad)
        assertTrue(result.trackHNoticeLoad)
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        assertNull(result.blockedReason)
    }
}
