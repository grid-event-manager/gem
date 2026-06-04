package org.hostess.apps.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidCompatibilityProbeTest {
    @Test
    fun `probe constructs core path and loads adapter boundary classes`() {
        val result = AndroidCompatibilityProbe().run()

        assertEquals("passed", result.status)
        assertTrue(result.coreCompile)
        assertTrue(result.adapterLoad)
        assertEquals("external_guard_required", result.forbiddenApiScan)
        assertNull(result.blockedReason)
    }
}
