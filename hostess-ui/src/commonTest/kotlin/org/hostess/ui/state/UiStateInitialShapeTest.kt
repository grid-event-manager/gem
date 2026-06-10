package org.hostess.ui.state

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.text.HostessTextKey
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
        assertEquals(HostessTextKey.Offline, state.sessionStrip.statusKey)
        assertFalse(state.sendFooter.visible)
        assertNull(state.sendFooter.statusTextKey)
        assertFalse(state.sendFooter.enabled)
        assertNull(state.session)
    }

    @Test
    fun loginAndSettingsStateProjectCredentialRuntimeWithoutStoringService() {
        val runtime = FakeHostessUiRuntime.ready()
        val readyState = runtime.credentialRuntimeState as HostessCredentialRuntimeReady

        val login = LoginUiState.fromCredentialRuntime(readyState)
        val settings = SettingsUiState.fromCredentialRuntime(readyState)

        assertEquals(CredentialRuntimeUiStatus.READY, login.credentialRuntime.status)
        assertTrue(login.credentialRuntime.ready)
        assertEquals(CredentialRuntimeUiStatus.READY, settings.credentialRuntime.status)
        assertTrue(settings.credentialRuntime.ready)
    }

    @Test
    fun unavailableCredentialRuntimeProjectsBlockedUiState() {
        val runtime = FakeHostessUiRuntime.unavailable()
        val login = LoginUiState.fromCredentialRuntime(runtime.credentialRuntimeState)

        assertEquals(CredentialRuntimeUiStatus.UNAVAILABLE, login.credentialRuntime.status)
        assertFalse(login.credentialRuntime.ready)
        assertEquals("credential runtime unavailable", login.credentialRuntime.message)
    }

    @Test
    fun routeLocalStateStartsUnselectedAndCollapsed() {
        val profileId = AccountProfileId("profile:v1:test")
        val login = LoginUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)
        val settings = SettingsUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)

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
        assertTrue(inventory.shortcuts.landmarksSelected)
        assertNull(inventory.selectedAttachment)
        assertEquals(HostessTextKey.BlankStatus, footer.statusTextKey)
        assertEquals(HostessTextKey.SendNotices, footer.primaryLabelKey)
        assertFalse(footer.enabled)
    }
}
