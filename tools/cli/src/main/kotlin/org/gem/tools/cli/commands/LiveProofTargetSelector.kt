package org.gem.tools.cli.commands

import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.services.TargetSelectionService

internal class LiveProofTargetSelector(
    private val targetSelectionService: TargetSelectionService,
) {
    fun select(groups: List<GroupMembership>, displayNames: List<String>): LiveProofTargetSelection {
        var targetSet = targetSelectionService.emptyTargetSet(groups)
        for (displayName in displayNames) {
            targetSet = when (val result = targetSelectionService.addTargetByDisplayName(targetSet, displayName)) {
                is TargetSelectionResult.Changed -> result.targetSet
                is TargetSelectionResult.Unchanged -> result.targetSet
                else -> return LiveProofTargetSelection.Failed("target group display name unavailable")
            }
        }
        if (targetSet.isEmpty()) {
            return LiveProofTargetSelection.Failed("target group display name unavailable")
        }
        return LiveProofTargetSelection.Selected(targetSet)
    }
}

internal sealed interface LiveProofTargetSelection {
    data class Selected(val targetSet: GroupTargetSet) : LiveProofTargetSelection
    data class Failed(val detail: String) : LiveProofTargetSelection
}
