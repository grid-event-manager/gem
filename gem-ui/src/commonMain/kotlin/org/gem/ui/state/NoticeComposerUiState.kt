package org.gem.ui.state

import org.gem.core.domain.NoticeDraftInvalidReason
import org.gem.ui.text.GemTextKey

data class NoticeComposerUiState(
    val subject: String = "",
    val body: String = "",
    val charCount: Int = 0,
    val draftValid: Boolean = false,
    val draftInvalidReasons: Set<NoticeDraftInvalidReason> = emptySet(),
    val selectedTargetSummary: GemTextKey = GemTextKey.SelectedCount(0),
    val selectedAttachmentSummary: GemTextKey = GemTextKey.None,
    val sendFooterState: SendFooterUiState = SendFooterUiState(),
    val sendAttempted: Boolean = false,
    val sentGroupCount: Int = 0,
    val failedGroupCount: Int = 0,
    val dispatchRejected: Boolean = false,
)
