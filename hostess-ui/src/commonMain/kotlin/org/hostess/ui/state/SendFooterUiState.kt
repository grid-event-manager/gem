package org.hostess.ui.state

import org.hostess.ui.text.HostessTextKey

data class SendFooterUiState(
    val visible: Boolean = true,
    val statusTextKey: HostessTextKey? = HostessTextKey.BlankStatus,
    val primaryLabelKey: HostessTextKey = HostessTextKey.SendNotices,
    val enabled: Boolean = false,
    val sending: Boolean = false,
)
