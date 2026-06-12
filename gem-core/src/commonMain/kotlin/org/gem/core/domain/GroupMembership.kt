package org.gem.core.domain

data class GroupMembership(
    val groupId: GroupId,
    val displayName: GroupDisplayName,
    val canSendNotices: Boolean,
    val acceptsNotices: Boolean?,
) {
    companion object {
        fun fromValues(
            groupId: String,
            displayName: String,
            canSendNotices: Boolean,
            acceptsNotices: Boolean?,
        ): GroupMembership = GroupMembership(
            groupId = GroupId(groupId),
            displayName = GroupDisplayName(displayName),
            canSendNotices = canSendNotices,
            acceptsNotices = acceptsNotices,
        )
    }
}
