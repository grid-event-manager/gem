package org.gem.ui.controllers

import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.navigation.SectionBackPolicy
import org.gem.ui.state.AppUiState
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.state.SessionStripUiState
import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextKey

class GemAppController(
    val runtime: GemUiRuntime,
    val state: AppUiState = AppUiState(),
) {
    fun openMenu(): GemAppController =
        copy(state.copy(menuOpen = true))

    fun closeMenu(): GemAppController =
        copy(state.copy(menuOpen = false))

    fun openSection(route: UiRoute): GemAppController =
        copy(state.copy(route = route, menuOpen = false))

    fun beginLogout(): GemAppController =
        copy(
            state.copy(
                menuOpen = false,
                blockingOperationMessageKey = GemTextKey.LoggingOut,
            ),
        )

    fun logout(): GemAppController {
        state.session?.let(runtime.sessionService::logout)
        return copy(clearedLoginState())
    }

    fun backFromSection(policy: SectionBackPolicy): GemAppController =
        when (policy) {
            SectionBackPolicy.ReturnToSessionOrLogin -> {
                if (state.session == null) {
                    copy(clearedLoginState())
                } else {
                    copy(state.copy(route = UiRoute.Compose, menuOpen = false))
                }
            }
            SectionBackPolicy.Root -> throw IllegalArgumentException("Root section back is not supported")
        }

    private fun clearedLoginState(): AppUiState =
        state.copy(
            route = UiRoute.Login,
            menuOpen = false,
            activeAccountLabel = "",
            sessionStrip = SessionStripUiState(
                visible = false,
                statusKey = GemTextKey.Offline,
                online = false,
                locationLabel = "",
            ),
            sendFooter = SendFooterUiState(visible = false, statusTextKey = null),
            operationMessageKey = GemTextKey.BlankStatus,
            blockingOperationMessageKey = null,
            session = null,
        )

    private fun copy(state: AppUiState): GemAppController =
        GemAppController(runtime, state)
}
