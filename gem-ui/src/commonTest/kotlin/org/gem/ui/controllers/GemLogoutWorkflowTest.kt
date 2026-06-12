package org.gem.ui.controllers

import kotlin.test.Test
import kotlin.test.assertEquals

class GemLogoutWorkflowTest {
    @Test
    fun `plain logout without session does not request exit`() {
        val decision = GemLogoutWorkflow.decide(
            hasActiveSession = false,
            logoutInFlight = false,
            exitAfterLogout = false,
            existingExitAfterCurrentLogout = false,
        )

        assertEquals(GemLogoutWorkflowAction.EXIT_IMMEDIATELY, decision.action)
        assertEquals(false, decision.exitAfterLogout)
    }

    @Test
    fun `window close without session exits immediately`() {
        val decision = GemLogoutWorkflow.decide(
            hasActiveSession = false,
            logoutInFlight = false,
            exitAfterLogout = true,
            existingExitAfterCurrentLogout = false,
        )

        assertEquals(GemLogoutWorkflowAction.EXIT_IMMEDIATELY, decision.action)
        assertEquals(true, decision.exitAfterLogout)
    }

    @Test
    fun `window close with active session starts logout before exit`() {
        val decision = GemLogoutWorkflow.decide(
            hasActiveSession = true,
            logoutInFlight = false,
            exitAfterLogout = true,
            existingExitAfterCurrentLogout = false,
        )

        assertEquals(GemLogoutWorkflowAction.START_LOGOUT, decision.action)
        assertEquals(true, decision.exitAfterLogout)
    }

    @Test
    fun `second close request during logout preserves exit after current logout`() {
        val decision = GemLogoutWorkflow.decide(
            hasActiveSession = true,
            logoutInFlight = true,
            exitAfterLogout = true,
            existingExitAfterCurrentLogout = false,
        )

        assertEquals(GemLogoutWorkflowAction.MERGE_WITH_IN_FLIGHT_LOGOUT, decision.action)
        assertEquals(true, decision.exitAfterLogout)
    }
}
