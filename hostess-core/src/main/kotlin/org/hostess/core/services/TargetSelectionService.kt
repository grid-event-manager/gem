package org.hostess.core.services

import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.TargetSelectionResult

class TargetSelectionService {
    fun emptyTargetSet(groups: Iterable<GroupMembership>): GroupTargetSet = GroupTargetSet.from(groups)

    fun addTarget(targetSet: GroupTargetSet, displayName: GroupDisplayName): TargetSelectionResult =
        targetSet.add(displayName)

    fun removeTarget(targetSet: GroupTargetSet, displayName: GroupDisplayName): TargetSelectionResult =
        targetSet.remove(displayName)

    fun addAllSendable(targetSet: GroupTargetSet): TargetSelectionResult = targetSet.addAllSendable()

    fun removeAll(targetSet: GroupTargetSet): TargetSelectionResult = targetSet.removeAll()
}
