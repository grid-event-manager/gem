package org.gem.ui.state

import org.gem.ui.text.GemTextKey

data class SendFooterUiState(
    val visible: Boolean = true,
    val statusTextKey: GemTextKey? = GemTextKey.BlankStatus,
    val missingRequirementKeys: List<GemTextKey> = DefaultMissingRequirementKeys,
    val showMissingRequirements: Boolean = false,
    val failureDetails: List<SendFailureDetailUiState> = emptyList(),
    val failureDetailsExpanded: Boolean = false,
    val primaryLabelKey: GemTextKey = GemTextKey.SendNotices,
    val enabled: Boolean = false,
    val sending: Boolean = false,
)

data class SendFailureDetailUiState(
    val groupName: String,
    val reasonKey: GemTextKey,
)

val DefaultMissingRequirementKeys: List<GemTextKey> = listOf(
    GemTextKey.MissingSubject,
    GemTextKey.MissingBody,
    GemTextKey.MissingGroups,
)
