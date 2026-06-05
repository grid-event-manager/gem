package org.hostess.core.domain

class GroupTargetSet private constructor(
    memberships: List<GroupMembership>,
    selectedIds: Set<GroupId>,
) {
    private val memberships: List<GroupMembership> = memberships.toList()
    private val selectedIds: Set<GroupId> = selectedIds.toSet()

    init {
        require(this.memberships.map { it.groupId }.toSet().size == this.memberships.size) {
            "Group memberships must have unique group IDs."
        }
    }

    val availableGroups: List<GroupMembership>
        get() = memberships

    val selectedGroups: List<GroupMembership>
        get() = memberships.filter { it.groupId in selectedIds }

    val selectedCount: Int
        get() = selectedIds.size

    fun isEmpty(): Boolean = selectedIds.isEmpty()

    fun isSelected(groupId: GroupId): Boolean = groupId in selectedIds

    fun add(displayName: GroupDisplayName): TargetSelectionResult {
        val matchResult = uniqueMatch(displayName)
        if (matchResult !is MatchResult.One) {
            return matchResult.toSelectionResult(this, displayName)
        }

        val group = matchResult.group
        if (!group.canSendNotices) {
            return TargetSelectionResult.CannotSendNotice(group, this)
        }
        if (group.groupId in selectedIds) {
            return TargetSelectionResult.Unchanged(this)
        }

        return TargetSelectionResult.Changed(copy(selectedIds + group.groupId))
    }

    fun remove(displayName: GroupDisplayName): TargetSelectionResult {
        val matchResult = uniqueMatch(displayName)
        if (matchResult !is MatchResult.One) {
            return matchResult.toSelectionResult(this, displayName)
        }

        val group = matchResult.group
        if (group.groupId !in selectedIds) {
            return TargetSelectionResult.Unchanged(this)
        }

        return TargetSelectionResult.Changed(copy(selectedIds - group.groupId))
    }

    fun addAllSendable(): TargetSelectionResult {
        val sendableIds = memberships
            .filter { it.canSendNotices }
            .mapTo(linkedSetOf()) { it.groupId }

        if (sendableIds == selectedIds) {
            return TargetSelectionResult.Unchanged(this)
        }

        return TargetSelectionResult.Changed(copy(sendableIds))
    }

    fun removeAll(): TargetSelectionResult {
        if (selectedIds.isEmpty()) {
            return TargetSelectionResult.Unchanged(this)
        }

        return TargetSelectionResult.Changed(copy(emptySet()))
    }

    private fun uniqueMatch(displayName: GroupDisplayName): MatchResult {
        val matches = memberships.filter { it.displayName == displayName }
        return when (matches.size) {
            0 -> MatchResult.None
            1 -> MatchResult.One(matches.single())
            else -> MatchResult.Many(matches)
        }
    }

    private fun copy(selectedIds: Set<GroupId>): GroupTargetSet = GroupTargetSet(memberships, selectedIds)

    private sealed interface MatchResult {
        data object None : MatchResult
        data class One(val group: GroupMembership) : MatchResult
        data class Many(val groups: List<GroupMembership>) : MatchResult

        fun toSelectionResult(targetSet: GroupTargetSet, displayName: GroupDisplayName): TargetSelectionResult =
            when (this) {
                None -> TargetSelectionResult.NoSuchGroup(displayName, targetSet)
                is One -> error("Single match must be handled by caller.")
                is Many -> TargetSelectionResult.AmbiguousDisplayName(displayName, groups, targetSet)
            }
    }

    companion object {
        fun from(memberships: Iterable<GroupMembership>): GroupTargetSet =
            GroupTargetSet(memberships.toList(), emptySet())
    }
}

sealed interface TargetSelectionResult {
    val targetSet: GroupTargetSet

    data class Changed(override val targetSet: GroupTargetSet) : TargetSelectionResult
    data class NoSuchGroup(
        val displayName: GroupDisplayName,
        override val targetSet: GroupTargetSet,
    ) : TargetSelectionResult

    data class AmbiguousDisplayName(
        val displayName: GroupDisplayName,
        val matches: List<GroupMembership>,
        override val targetSet: GroupTargetSet,
    ) : TargetSelectionResult

    data class CannotSendNotice(
        val group: GroupMembership,
        override val targetSet: GroupTargetSet,
    ) : TargetSelectionResult

    data class Unchanged(override val targetSet: GroupTargetSet) : TargetSelectionResult
}
