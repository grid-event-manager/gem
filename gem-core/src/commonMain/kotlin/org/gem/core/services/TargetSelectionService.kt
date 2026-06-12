package org.gem.core.services

import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.TargetSelectionResult

class TargetSelectionService {
    fun emptyTargetSet(groups: Iterable<GroupMembership>): GroupTargetSet = GroupTargetSet.from(groups)

    fun addTarget(targetSet: GroupTargetSet, displayName: GroupDisplayName): TargetSelectionResult =
        targetSet.add(displayName)

    fun addTargetByDisplayName(targetSet: GroupTargetSet, displayName: String): TargetSelectionResult =
        addTarget(targetSet, GroupDisplayName(displayName))

    fun removeTarget(targetSet: GroupTargetSet, displayName: GroupDisplayName): TargetSelectionResult =
        targetSet.remove(displayName)

    fun removeTargetByDisplayName(targetSet: GroupTargetSet, displayName: String): TargetSelectionResult =
        removeTarget(targetSet, GroupDisplayName(displayName))

    fun addAllSendable(targetSet: GroupTargetSet): TargetSelectionResult = targetSet.addAllSendable()

    fun removeAll(targetSet: GroupTargetSet): TargetSelectionResult = targetSet.removeAll()
}
