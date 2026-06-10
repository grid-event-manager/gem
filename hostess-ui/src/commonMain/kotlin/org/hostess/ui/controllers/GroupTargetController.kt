package org.hostess.ui.controllers

import org.hostess.core.domain.GroupId
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.GroupTargetMode
import org.hostess.ui.state.GroupTargetUiState

class GroupTargetController(
    val runtime: HostessUiRuntime,
    val state: GroupTargetUiState = GroupTargetUiState(),
) {
    fun selectAllGroupsMode(): GroupTargetController {
        // B-09 owns TargetSelectionService.addAllSendable delegation.
        return copy(state.copy(mode = GroupTargetMode.ALL, pickerVisible = false))
    }

    fun selectManualGroupsMode(): GroupTargetController {
        // B-09 owns TargetSelectionService mutation; B-06 owns mode visibility shape only.
        return copy(state.copy(mode = GroupTargetMode.MANUAL, pickerVisible = true))
    }

    fun setManualGroupSelected(
        groupId: GroupId,
        selected: Boolean,
    ): GroupTargetController {
        // B-09 owns display-name/core target-set mutation; B-06 toggles row projection only.
        val rows = state.rows.map { row ->
            if (row.groupId == groupId) {
                row.copy(selected = selected)
            } else {
                row
            }
        }
        return copy(
            state.copy(
                mode = GroupTargetMode.MANUAL,
                rows = rows,
                selectedCount = rows.count { it.selected },
                pickerVisible = true,
            ),
        )
    }

    private fun copy(state: GroupTargetUiState): GroupTargetController =
        GroupTargetController(runtime, state)
}
