package org.hostess.apps.android

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidCompatibilityProbeTest {
    @Test
    fun `probe constructs core path and loads adapter boundary classes`() {
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
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        assertNull(result.blockedReason)
    }

    @Test
    fun `android gap result preserves no live contact and no ui contract`() {
        val result = AndroidCompatibilityResult.androidGap(
            coreCompile = true,
            adapterLoad = false,
            runtimeLoad = false,
            transportLoad = false,
            trackCClassLoad = false,
            trackDComplianceLoad = true,
            trackDsLoginPackageLoad = true,
            trackGGridLoad = false,
            reason = "Android compatibility probe failed lanes=adapterLoad,runtimeLoad,transportLoad,trackGGridLoad",
        )

        assertEquals("android_gap", result.status)
        assertTrue(result.coreCompile)
        assertFalse(result.adapterLoad)
        assertFalse(result.runtimeLoad)
        assertFalse(result.transportLoad)
        assertFalse(result.trackCClassLoad)
        assertTrue(result.trackDComplianceLoad)
        assertTrue(result.trackDsLoginPackageLoad)
        assertFalse(result.trackGGridLoad)
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        val reason = assertNotNull(result.blockedReason)
        assertContains(reason, "adapterLoad")
        assertContains(reason, "runtimeLoad")
        assertContains(reason, "transportLoad")
        assertContains(reason, "trackGGridLoad")
    }
}
