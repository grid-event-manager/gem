package org.gem.ui.state

import org.gem.core.domain.GemSession
import org.gem.ui.text.GemTextKey

data class AppUiState(
    val route: UiRoute = UiRoute.Login,
    val menuOpen: Boolean = false,
    val activeAccountLabel: String = "",
    val sessionStrip: SessionStripUiState = SessionStripUiState(),
    val sendFooter: SendFooterUiState = SendFooterUiState(visible = false, statusTextKey = null),
    val operationMessageKey: GemTextKey? = null,
    val blockingOperationMessageKey: GemTextKey? = null,
    val session: GemSession? = null,
)

data class SessionStripUiState(
    val visible: Boolean = false,
    val locationLabel: String = "",
    val statusKey: GemTextKey = GemTextKey.Offline,
    val online: Boolean = false,
)
