package org.hostess.ui.controllers

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.GroupTargetMode
import org.hostess.ui.state.GroupTargetRowUiState
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.text.HostessTextKey

class GroupTargetController(
    val runtime: HostessUiRuntime,
    val state: GroupTargetUiState = GroupTargetUiState(),
    val targetSet: GroupTargetSet = GroupTargetSet.from(emptyList()),
) {
    fun selectAllGroupsMode(): GroupTargetController {
        val result = runtime.targetSelectionService.addAllSendable(targetSet)
        return applySelection(result, GroupTargetMode.ALL, pickerVisible = false)
    }

    fun clearGroupSelectionMode(): GroupTargetController {
        val result = runtime.targetSelectionService.removeAll(targetSet)
        return applySelection(result, GroupTargetMode.NONE, pickerVisible = false)
    }

    fun selectManualGroupsMode(): GroupTargetController {
        val nextTargetSet = if (state.mode == GroupTargetMode.ALL) {
            runtime.targetSelectionService.removeAll(targetSet).targetSet
        } else {
            targetSet
        }
        return copy(
            state = projectState(nextTargetSet, GroupTargetMode.MANUAL, pickerVisible = true),
            targetSet = nextTargetSet,
        )
    }

    fun clearManualGroupsMode(): GroupTargetController {
        val result = runtime.targetSelectionService.removeAll(targetSet)
        return applySelection(result, GroupTargetMode.NONE, pickerVisible = false)
    }

    fun setManualGroupSelected(
        displayName: String,
        selected: Boolean,
    ): GroupTargetController {
        val result = if (selected) {
            runtime.targetSelectionService.addTargetByDisplayName(targetSet, displayName)
        } else {
            runtime.targetSelectionService.removeTargetByDisplayName(targetSet, displayName)
        }
        return applySelection(result, GroupTargetMode.MANUAL, pickerVisible = true)
    }

    private fun applySelection(
        result: TargetSelectionResult,
        mode: GroupTargetMode,
        pickerVisible: Boolean,
    ): GroupTargetController {
        val errorKey = when (result) {
            is TargetSelectionResult.AmbiguousDisplayName,
            is TargetSelectionResult.CannotSendNotice,
            is TargetSelectionResult.NoSuchGroup,
            -> HostessTextKey.BlankStatus
            is TargetSelectionResult.Changed,
            is TargetSelectionResult.Unchanged,
            -> null
        }
        return copy(
            state = projectState(result.targetSet, mode, pickerVisible, errorKey),
            targetSet = result.targetSet,
        )
    }

    private fun projectState(
        targetSet: GroupTargetSet,
        mode: GroupTargetMode,
        pickerVisible: Boolean,
        errorKey: HostessTextKey? = null,
    ): GroupTargetUiState =
        projectTargetState(targetSet, mode, pickerVisible, errorKey)

    private fun copy(
        state: GroupTargetUiState,
        targetSet: GroupTargetSet = this.targetSet,
    ): GroupTargetController =
        GroupTargetController(runtime, state, targetSet)

    companion object {
        fun fromGroups(
            runtime: HostessUiRuntime,
            groups: List<GroupMembership>,
        ): GroupTargetController {
            val targetSet = runtime.targetSelectionService.emptyTargetSet(groups)
            return GroupTargetController(
                runtime = runtime,
                state = projectTargetState(targetSet, GroupTargetMode.NONE, pickerVisible = false),
                targetSet = targetSet,
            )
        }
    }
}

private fun projectTargetState(
    targetSet: GroupTargetSet,
    mode: GroupTargetMode,
    pickerVisible: Boolean,
    errorKey: HostessTextKey? = null,
): GroupTargetUiState =
    GroupTargetUiState(
        mode = mode,
        rows = targetSet.availableGroups.map { group ->
            GroupTargetRowUiState(
                groupId = group.groupId,
                displayName = group.displayName.value,
                canSendNotices = group.canSendNotices,
                selected = targetSet.isSelected(group.groupId),
            )
        },
        selectedCount = targetSet.selectedCount,
        pickerVisible = pickerVisible,
        errorKey = errorKey,
    )
