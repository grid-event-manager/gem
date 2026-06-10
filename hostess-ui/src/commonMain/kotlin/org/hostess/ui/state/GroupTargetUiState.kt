package org.hostess.ui.state

import org.hostess.core.domain.GroupId

data class GroupTargetUiState(
    val mode: GroupTargetMode = GroupTargetMode.NONE,
    val rows: List<GroupTargetRowUiState> = emptyList(),
    val selectedCount: Int = 0,
    val pickerVisible: Boolean = false,
)

data class GroupTargetRowUiState(
    val groupId: GroupId,
    val displayName: String,
    val canSendNotices: Boolean,
    val selected: Boolean = false,
)

enum class GroupTargetMode {
    NONE,
    ALL,
    MANUAL,
}
