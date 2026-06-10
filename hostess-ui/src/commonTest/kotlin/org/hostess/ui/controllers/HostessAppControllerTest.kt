package org.hostess.ui.controllers

import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.SendFooterUiState
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HostessAppControllerTest {
    private val runtime = FakeHostessUiRuntime.ready()

    @Test
    fun `logout clears session surfaces and returns to login`() {
        val controller = HostessAppController(
            runtime = runtime,
            state = loggedInSettingsState(),
        )

        val loggedOut = controller.logout().state

        assertEquals(UiRoute.Login, loggedOut.route)
        assertFalse(loggedOut.menuOpen)
        assertEquals("", loggedOut.activeAccountLabel)
        assertEquals(SessionStripUiState(visible = false, statusKey = HostessTextKey.Offline), loggedOut.sessionStrip)
        assertEquals(SendFooterUiState(visible = false, statusTextKey = null), loggedOut.sendFooter)
        assertEquals(HostessTextKey.BlankStatus, loggedOut.operationMessageKey)
        assertNull(loggedOut.session)
    }

    @Test
    fun `settings back returns to compose only with active session`() {
        val backed = HostessAppController(runtime, loggedInSettingsState()).backFromSettings().state

        assertEquals(UiRoute.Compose, backed.route)
        assertFalse(backed.menuOpen)
        assertEquals(FakeInventoryFixtures.session(), backed.session)
    }

    @Test
    fun `settings back without active session returns to login`() {
        val backed = HostessAppController(
            runtime = runtime,
            state = AppUiState(route = UiRoute.Settings, menuOpen = true),
        ).backFromSettings().state

        assertEquals(UiRoute.Login, backed.route)
        assertFalse(backed.menuOpen)
        assertNull(backed.session)
        assertFalse(backed.sendFooter.visible)
        assertFalse(backed.sessionStrip.visible)
    }

    private fun loggedInSettingsState(): AppUiState =
        AppUiState(
            route = UiRoute.Settings,
            menuOpen = true,
            activeAccountLabel = "jackraybold resident",
            sessionStrip = SessionStripUiState(
                visible = true,
                locationLabel = "London City",
                statusKey = HostessTextKey.Online,
                online = true,
            ),
            sendFooter = SendFooterUiState(visible = true, statusTextKey = HostessTextKey.Ready, enabled = true),
            operationMessageKey = HostessTextKey.Ready,
            session = FakeInventoryFixtures.session(),
        )
}
