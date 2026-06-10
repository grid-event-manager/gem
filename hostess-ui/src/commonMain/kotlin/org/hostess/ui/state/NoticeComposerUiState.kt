package org.hostess.ui.state

import org.hostess.ui.text.HostessTextKey

data class NoticeComposerUiState(
    val subject: String = "",
    val body: String = "",
    val charCount: Int = 0,
    val selectedTargetSummary: HostessTextKey = HostessTextKey.SelectedCount(0),
    val selectedAttachmentSummary: String = "",
    val sendFooterState: SendFooterUiState = SendFooterUiState(),
)
