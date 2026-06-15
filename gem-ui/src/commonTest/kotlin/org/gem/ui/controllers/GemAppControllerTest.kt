package org.gem.ui.controllers

import org.gem.ui.state.AppUiState
import org.gem.ui.navigation.SectionBackPolicy
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.state.SessionStripUiState
import org.gem.ui.state.UiRoute
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeInventoryFixtures
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class GemAppControllerTest {
    private val runtime = FakeGemUiRuntime.ready()

    @Test
    fun `logout clears session surfaces and returns to login`() {
        val controller = GemAppController(
            runtime = runtime,
            state = loggedInSectionState(UiRoute.Settings),
        )

        val loggedOut = controller.logout().state

        assertEquals(UiRoute.Login, loggedOut.route)
        assertFalse(loggedOut.menuOpen)
        assertEquals("", loggedOut.activeAccountLabel)
        assertEquals(SessionStripUiState(visible = false, statusKey = GemTextKey.Offline), loggedOut.sessionStrip)
        assertEquals(SendFooterUiState(visible = false, statusTextKey = null), loggedOut.sendFooter)
        assertEquals(GemTextKey.BlankStatus, loggedOut.operationMessageKey)
        assertNull(loggedOut.session)
    }

    @Test
    fun `section back returns to compose only with active session`() {
        val backed = GemAppController(
            runtime,
            loggedInSectionState(UiRoute.Accounts),
        ).backFromSection(SectionBackPolicy.ReturnToSessionOrLogin).state

        assertEquals(UiRoute.Compose, backed.route)
        assertFalse(backed.menuOpen)
        assertEquals(FakeInventoryFixtures.session(), backed.session)
    }

    @Test
    fun `section back without active session returns to login`() {
        val backed = GemAppController(
            runtime = runtime,
            state = AppUiState(route = UiRoute.Settings, menuOpen = true),
        ).backFromSection(SectionBackPolicy.ReturnToSessionOrLogin).state

        assertEquals(UiRoute.Login, backed.route)
        assertFalse(backed.menuOpen)
        assertNull(backed.session)
        assertFalse(backed.sendFooter.visible)
        assertFalse(backed.sessionStrip.visible)
    }

    @Test
    fun `root section back is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            GemAppController(runtime).backFromSection(SectionBackPolicy.Root)
        }

        assertEquals("Root section back is not supported", failure.message)
    }

    private fun loggedInSectionState(route: UiRoute): AppUiState =
        AppUiState(
            route = route,
            menuOpen = true,
            activeAccountLabel = "venuehost resident",
            sessionStrip = SessionStripUiState(
                visible = true,
                locationLabel = "London City",
                statusKey = GemTextKey.Online,
                online = true,
            ),
            sendFooter = SendFooterUiState(visible = true, statusTextKey = GemTextKey.Ready, enabled = true),
            operationMessageKey = GemTextKey.Ready,
            session = FakeInventoryFixtures.session(),
        )
}
