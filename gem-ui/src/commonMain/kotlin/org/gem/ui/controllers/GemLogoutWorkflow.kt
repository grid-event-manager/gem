package org.gem.ui.controllers

internal enum class GemLogoutWorkflowAction {
    EXIT_IMMEDIATELY,
    START_LOGOUT,
    MERGE_WITH_IN_FLIGHT_LOGOUT,
}

internal data class GemLogoutWorkflowDecision(
    val action: GemLogoutWorkflowAction,
    val exitAfterLogout: Boolean,
)

internal object GemLogoutWorkflow {
    fun decide(
        hasActiveSession: Boolean,
        logoutInFlight: Boolean,
        exitAfterLogout: Boolean,
        existingExitAfterCurrentLogout: Boolean,
    ): GemLogoutWorkflowDecision {
        val mergedExit = existingExitAfterCurrentLogout || exitAfterLogout
        return when {
            !hasActiveSession -> GemLogoutWorkflowDecision(
                action = GemLogoutWorkflowAction.EXIT_IMMEDIATELY,
                exitAfterLogout = exitAfterLogout,
            )
            logoutInFlight -> GemLogoutWorkflowDecision(
                action = GemLogoutWorkflowAction.MERGE_WITH_IN_FLIGHT_LOGOUT,
                exitAfterLogout = mergedExit,
            )
            else -> GemLogoutWorkflowDecision(
                action = GemLogoutWorkflowAction.START_LOGOUT,
                exitAfterLogout = mergedExit,
            )
        }
    }
}
