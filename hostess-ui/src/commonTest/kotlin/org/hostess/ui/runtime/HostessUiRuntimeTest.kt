package org.hostess.ui.runtime

import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.ui.testing.FakeHostessUiRuntime
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostessUiRuntimeTest {
    @Test
    fun readyRuntimeExposesCredentialServicesThroughNarrowAccessors() {
        val runtime = FakeHostessUiRuntime.ready()

        assertTrue(runtime.credentialRuntimeState is HostessCredentialRuntimeReady)
        assertNotNull(runtime.credentialServiceOrNull())
        assertNotNull(runtime.savedLoginAuthenticationServiceOrNull())
    }

    @Test
    fun unavailableRuntimeDoesNotExposeCredentialServices() {
        val runtime = FakeHostessUiRuntime.unavailable()

        assertNull(runtime.credentialServiceOrNull())
        assertNull(runtime.savedLoginAuthenticationServiceOrNull())
    }

    @Test
    fun complianceProviderIsSuppliedByRuntimeComposition() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()

        val request = runtime.loginComplianceProvider.requestFor(profile)

        assertTrue(request.proofAccountAttested)
        assertTrue(request.scriptedAgentAttested)
    }
}
