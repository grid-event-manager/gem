package org.gem.apps.android

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
        assertTrue(result.vaultRuntimeLoad)
        assertTrue(result.protocolAdapterLoad)
        assertTrue(result.loginComplianceLoad)
        assertTrue(result.loginPackageSerializationLoad)
        assertTrue(result.inventoryCapabilityLoad)
        assertTrue(result.noticeProtocolLoad)
        assertTrue(result.avatarReadinessLoad)
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
            vaultRuntimeLoad = false,
            protocolAdapterLoad = false,
            loginComplianceLoad = true,
            loginPackageSerializationLoad = true,
            inventoryCapabilityLoad = false,
            noticeProtocolLoad = false,
            avatarReadinessLoad = false,
            reason = "Android compatibility probe failed lanes=adapterLoad,runtimeLoad,transportLoad,vaultRuntimeLoad,inventoryCapabilityLoad,noticeProtocolLoad,avatarReadinessLoad",
        )

        assertEquals("android_gap", result.status)
        assertTrue(result.coreCompile)
        assertFalse(result.adapterLoad)
        assertFalse(result.runtimeLoad)
        assertFalse(result.transportLoad)
        assertFalse(result.vaultRuntimeLoad)
        assertFalse(result.protocolAdapterLoad)
        assertTrue(result.loginComplianceLoad)
        assertTrue(result.loginPackageSerializationLoad)
        assertFalse(result.inventoryCapabilityLoad)
        assertFalse(result.noticeProtocolLoad)
        assertFalse(result.avatarReadinessLoad)
        assertTrue(result.noLiveGridContact)
        assertTrue(result.noUiSurface)
        val reason = assertNotNull(result.blockedReason)
        assertContains(reason, "adapterLoad")
        assertContains(reason, "runtimeLoad")
        assertContains(reason, "transportLoad")
        assertContains(reason, "vaultRuntimeLoad")
        assertContains(reason, "inventoryCapabilityLoad")
        assertContains(reason, "noticeProtocolLoad")
        assertContains(reason, "avatarReadinessLoad")
    }
}
