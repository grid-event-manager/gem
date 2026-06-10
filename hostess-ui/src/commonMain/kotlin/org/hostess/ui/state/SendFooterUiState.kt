package org.hostess.ui.state

import org.hostess.ui.text.HostessTextKey

data class SendFooterUiState(
    val visible: Boolean = true,
    val statusTextKey: HostessTextKey? = HostessTextKey.BlankStatus,
    val missingRequirementKeys: List<HostessTextKey> = DefaultMissingRequirementKeys,
    val showMissingRequirements: Boolean = false,
    val primaryLabelKey: HostessTextKey = HostessTextKey.SendNotices,
    val enabled: Boolean = false,
    val sending: Boolean = false,
)

val DefaultMissingRequirementKeys: List<HostessTextKey> = listOf(
    HostessTextKey.MissingSubject,
    HostessTextKey.MissingBody,
    HostessTextKey.MissingGroups,
)
