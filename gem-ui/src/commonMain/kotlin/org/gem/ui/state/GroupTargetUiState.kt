package org.gem.ui.state

import org.gem.core.domain.GroupId
import org.gem.ui.text.GemTextKey

data class GroupTargetUiState(
    val loading: Boolean = true,
    val mode: GroupTargetMode = GroupTargetMode.NONE,
    val rows: List<GroupTargetRowUiState> = emptyList(),
    val selectedCount: Int = 0,
    val pickerVisible: Boolean = false,
    val errorKey: GemTextKey? = null,
)

data class GroupTargetRowUiState(
    val groupId: GroupId,
    val displayName: String,
    val selected: Boolean = false,
)

enum class GroupTargetMode {
    NONE,
    ALL,
    MANUAL,
}
