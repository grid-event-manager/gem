package org.hostess.ui.state

import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.ui.text.HostessTextKey

data class NoticeComposerUiState(
    val subject: String = "",
    val body: String = "",
    val charCount: Int = 0,
    val draftValid: Boolean = false,
    val draftInvalidReasons: Set<NoticeDraftInvalidReason> = emptySet(),
    val selectedTargetSummary: HostessTextKey = HostessTextKey.SelectedCount(0),
    val selectedAttachmentSummary: HostessTextKey = HostessTextKey.None,
    val sendFooterState: SendFooterUiState = SendFooterUiState(),
    val sendAttempted: Boolean = false,
    val sentGroupCount: Int = 0,
    val failedGroupCount: Int = 0,
    val dispatchRejected: Boolean = false,
)
