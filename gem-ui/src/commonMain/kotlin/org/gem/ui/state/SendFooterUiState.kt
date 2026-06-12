package org.gem.ui.state

import org.gem.ui.text.GemTextKey

data class SendFooterUiState(
    val visible: Boolean = true,
    val statusTextKey: GemTextKey? = GemTextKey.BlankStatus,
    val missingRequirementKeys: List<GemTextKey> = DefaultMissingRequirementKeys,
    val showMissingRequirements: Boolean = false,
    val detailText: String? = null,
    val primaryLabelKey: GemTextKey = GemTextKey.SendNotices,
    val enabled: Boolean = false,
    val sending: Boolean = false,
)

val DefaultMissingRequirementKeys: List<GemTextKey> = listOf(
    GemTextKey.MissingSubject,
    GemTextKey.MissingBody,
    GemTextKey.MissingGroups,
)
