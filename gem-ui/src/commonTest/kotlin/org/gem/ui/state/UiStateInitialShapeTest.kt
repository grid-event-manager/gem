package org.gem.ui.state

import org.gem.core.domain.AccountProfileId
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiStateInitialShapeTest {
    @Test
    fun appStateStartsOnLoginWithHiddenOfflineSessionStrip() {
        val state = AppUiState()

        assertEquals(UiRoute.Login, state.route)
        assertFalse(state.menuOpen)
        assertEquals("", state.activeAccountLabel)
        assertFalse(state.sessionStrip.visible)
        assertEquals("", state.sessionStrip.locationLabel)
        assertEquals(GemTextKey.Offline, state.sessionStrip.statusKey)
        assertFalse(state.sendFooter.visible)
        assertNull(state.sendFooter.statusTextKey)
        assertFalse(state.sendFooter.enabled)
        assertNull(state.session)
    }

    @Test
    fun loginAndSettingsStateProjectCredentialRuntimeWithoutStoringService() {
        val runtime = FakeGemUiRuntime.ready()
        val readyState = runtime.credentialRuntimeState as GemCredentialRuntimeReady

        val login = LoginUiState.fromCredentialRuntime(readyState)
        val settings = SettingsUiState.fromCredentialRuntime(readyState)

        assertEquals(CredentialRuntimeUiStatus.READY, login.credentialRuntime.status)
        assertTrue(login.credentialRuntime.ready)
        assertEquals(CredentialRuntimeUiStatus.READY, settings.credentialRuntime.status)
        assertTrue(settings.credentialRuntime.ready)
    }

    @Test
    fun unavailableCredentialRuntimeProjectsBlockedUiState() {
        val runtime = FakeGemUiRuntime.unavailable()
        val login = LoginUiState.fromCredentialRuntime(runtime.credentialRuntimeState)

        assertEquals(CredentialRuntimeUiStatus.UNAVAILABLE, login.credentialRuntime.status)
        assertFalse(login.credentialRuntime.ready)
        assertEquals("credential runtime unavailable", login.credentialRuntime.message)
    }

    @Test
    fun routeLocalStateStartsUnselectedAndCollapsed() {
        val profileId = AccountProfileId("profile:v1:test")
        val login = LoginUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)
        val settings = SettingsUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)

        assertNull(login.selectedProfileId)
        assertEquals("", login.usernameDraft)
        assertFalse(login.passwordEnabled)
        assertFalse(login.loginEnabled)
        assertFalse(login.operation.inFlight)
        assertEquals(emptySet(), settings.selectedDeleteProfileIds)
        assertFalse(settings.confirmDeleteOpen)
        assertFalse(settings.copy(selectedDeleteProfileIds = setOf(profileId)).confirmDeleteOpen)
    }

    @Test
    fun composerTargetInventoryAndFooterStateStartSafe() {
        val composer = NoticeComposerUiState()
        val groups = GroupTargetUiState()
        val inventory = InventoryBrowserUiState()
        val footer = SendFooterUiState()

        assertEquals(0, composer.charCount)
        assertEquals(GroupTargetMode.NONE, groups.mode)
        assertFalse(groups.pickerVisible)
        assertTrue(groups.loading)
        assertTrue(inventory.loading)
        assertTrue(inventory.shortcuts.landmarksSelected)
        assertNull(inventory.selectedAttachment)
        assertEquals(GemTextKey.BlankStatus, footer.statusTextKey)
        assertEquals(GemTextKey.SendNotices, footer.primaryLabelKey)
        assertFalse(footer.enabled)
    }
}
