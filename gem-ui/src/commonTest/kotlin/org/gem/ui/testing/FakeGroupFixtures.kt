package org.gem.ui.testing

import org.gem.core.domain.GroupMembership

object FakeGroupFixtures {
    val owks: GroupMembership = GroupMembership.fromValues(
        groupId = "group-owks",
        displayName = "Owks",
        canSendNotices = true,
        acceptsNotices = true,
    )
    val minx: GroupMembership = GroupMembership.fromValues(
        groupId = "group-minx",
        displayName = "m!nx",
        canSendNotices = true,
        acceptsNotices = true,
    )
    val audience: GroupMembership = GroupMembership.fromValues(
        groupId = "group-audience",
        displayName = "Audience",
        canSendNotices = false,
        acceptsNotices = true,
    )
    val duplicateOne: GroupMembership = GroupMembership.fromValues(
        groupId = "group-duplicate-one",
        displayName = "Duplicate",
        canSendNotices = true,
        acceptsNotices = true,
    )
    val duplicateTwo: GroupMembership = GroupMembership.fromValues(
        groupId = "group-duplicate-two",
        displayName = "Duplicate",
        canSendNotices = true,
        acceptsNotices = true,
    )

    fun sendableGroups(): List<GroupMembership> = listOf(owks, minx)

    fun mixedGroups(): List<GroupMembership> = listOf(owks, minx, audience)

    fun duplicateGroups(): List<GroupMembership> = listOf(duplicateOne, duplicateTwo)
}
