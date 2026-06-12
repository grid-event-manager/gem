package org.gem.ui.runtime

import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.ui.testing.FakeGemUiRuntime
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemUiRuntimeTest {
    @Test
    fun readyRuntimeExposesCredentialServicesThroughNarrowAccessors() {
        val runtime = FakeGemUiRuntime.ready()

        assertTrue(runtime.credentialRuntimeState is GemCredentialRuntimeReady)
        assertNotNull(runtime.credentialServiceOrNull())
        assertNotNull(runtime.savedLoginAuthenticationServiceOrNull())
        assertNotNull(runtime.loginProfileAuthenticationServiceOrNull())
        assertNotNull(runtime.savedAccountManagementServiceOrNull())
        assertTrue(runtime.themePreferenceService.loadPreference().preference.name.isNotBlank())
    }

    @Test
    fun unavailableRuntimeDoesNotExposeCredentialServices() {
        val runtime = FakeGemUiRuntime.unavailable()

        assertNull(runtime.credentialServiceOrNull())
        assertNull(runtime.savedLoginAuthenticationServiceOrNull())
        assertNull(runtime.loginProfileAuthenticationServiceOrNull())
        assertNull(runtime.savedAccountManagementServiceOrNull())
    }

    @Test
    fun complianceProviderIsSuppliedByRuntimeComposition() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()

        val request = runtime.loginComplianceProvider.requestFor(profile)

        assertTrue(request.proofAccountAttested)
        assertTrue(request.scriptedAgentAttested)
    }

    @Test
    fun themePreferenceServiceIsSuppliedByRuntimeComposition() {
        val runtime = FakeGemUiRuntime.ready()
        val snapshot = runtime.themePreferenceService.loadPreference()

        assertNull(snapshot.warning)
        assertTrue(snapshot.preference.name.isNotBlank())
    }
}
