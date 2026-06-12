package org.gem.ui.controllers

import org.gem.ui.runtime.GemUiRuntime
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

    fun openSettings(): GemAppController =
        copy(state.copy(route = UiRoute.Settings, menuOpen = false))

    fun beginLogout(): GemAppController =
        copy(
            state.copy(
                menuOpen = false,
                blockingOperationMessageKey = GemTextKey.LoggingOut,
            ),
        )

    fun logout(): GemAppController {
        state.session?.let(runtime.sessionService::logout)
        return copy(
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
            ),
        )
    }

    fun backFromSettings(): GemAppController {
        if (state.session == null) {
            return copy(
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
                ),
            )
        }
        return copy(state.copy(route = UiRoute.Compose, menuOpen = false))
    }

    private fun copy(state: AppUiState): GemAppController =
        GemAppController(runtime, state)
}
