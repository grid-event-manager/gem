package org.hostess.ui.controllers

import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.SendFooterUiState
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.text.HostessTextKey

class HostessAppController(
    val runtime: HostessUiRuntime,
    val state: AppUiState = AppUiState(),
) {
    fun openMenu(): HostessAppController =
        copy(state.copy(menuOpen = true))

    fun closeMenu(): HostessAppController =
        copy(state.copy(menuOpen = false))

    fun openSettings(): HostessAppController =
        copy(state.copy(route = UiRoute.Settings, menuOpen = false))

    fun logout(): HostessAppController {
        state.session?.let(runtime.sessionService::logout)
        return copy(
            state.copy(
                route = UiRoute.Login,
                menuOpen = false,
                activeAccountLabel = "",
                sessionStrip = SessionStripUiState(
                    visible = false,
                    statusKey = HostessTextKey.Offline,
                    online = false,
                    locationLabel = "",
                ),
                sendFooter = SendFooterUiState(visible = false, statusTextKey = null),
                operationMessageKey = HostessTextKey.BlankStatus,
                session = null,
            ),
        )
    }

    fun backFromSettings(): HostessAppController {
        if (state.session == null) {
            return copy(
                state.copy(
                    route = UiRoute.Login,
                    menuOpen = false,
                    activeAccountLabel = "",
                    sessionStrip = SessionStripUiState(
                        visible = false,
                        statusKey = HostessTextKey.Offline,
                        online = false,
                        locationLabel = "",
                    ),
                    sendFooter = SendFooterUiState(visible = false, statusTextKey = null),
                    operationMessageKey = HostessTextKey.BlankStatus,
                    session = null,
                ),
            )
        }
        return copy(state.copy(route = UiRoute.Compose, menuOpen = false))
    }

    private fun copy(state: AppUiState): HostessAppController =
        HostessAppController(runtime, state)
}
