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
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        assertEquals("external_guard_required", result.forbiddenApiScan)
        assertNull(result.blockedReason)
    }

    @Test
    fun `probe reports android gap without live contact when canonical runtime cannot load`() {
        val result = AndroidCompatibilityProbe {
            throw IllegalStateException("runtime unavailable")
        }.run()

        assertEquals("android_gap", result.status)
        assertTrue(result.coreCompile)
        assertFalse(result.adapterLoad)
        assertFalse(result.runtimeLoad)
        assertFalse(result.transportLoad)
        assertFalse(result.trackCClassLoad)
        assertTrue(result.trackDComplianceLoad)
        assertTrue(result.trackDsLoginPackageLoad)
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        assertEquals("external_guard_required", result.forbiddenApiScan)
        val reason = assertNotNull(result.blockedReason)
        assertContains(reason, "adapterLoad")
        assertContains(reason, "runtimeLoad")
        assertContains(reason, "transportLoad")
        assertContains(reason, "cause=IllegalStateException")
    }
}
