package org.hostess.ui.state

import org.hostess.core.domain.HostessSession
import org.hostess.ui.text.HostessTextKey

data class AppUiState(
    val route: UiRoute = UiRoute.Login,
    val menuOpen: Boolean = false,
    val activeAccountLabel: String = "",
    val sessionStrip: SessionStripUiState = SessionStripUiState(),
    val operationMessageKey: HostessTextKey? = null,
    val session: HostessSession? = null,
)

data class SessionStripUiState(
    val visible: Boolean = false,
    val locationLabel: String = "",
    val statusKey: HostessTextKey = HostessTextKey.Offline,
    val online: Boolean = false,
)
