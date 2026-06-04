package org.hostess.core.domain

data class GroupMembership(
    val groupId: GroupId,
    val displayName: GroupDisplayName,
    val canSendNotices: Boolean,
    val acceptsNotices: Boolean?,
)
