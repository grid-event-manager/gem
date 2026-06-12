package org.gem.protocol.libomv

import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus

internal object LibomvMapping {
    const val SEND_NOTICES_POWER: Long = 1L shl 42

    fun groupMembership(snapshot: LibomvGroupSnapshot): GroupMembership = GroupMembership(
        groupId = GroupId(snapshot.groupId),
        displayName = GroupDisplayName(snapshot.displayName),
        canSendNotices = snapshot.powers and SEND_NOTICES_POWER != 0L,
        acceptsNotices = snapshot.acceptsNotices,
    )

    fun groupNoticeStatus(snapshot: LibomvNoticeStatusSnapshot): GroupSendStatus = GroupSendStatus(
        group = groupMembership(snapshot.group),
        state = if (snapshot.delivered) GroupSendState.SENT else GroupSendState.FAILED,
        detail = snapshot.detail,
    )
}

internal data class LibomvGroupSnapshot(
    val groupId: String,
    val displayName: String,
    val powers: Long,
    val acceptsNotices: Boolean?,
)

internal data class LibomvNoticeStatusSnapshot(
    val group: LibomvGroupSnapshot,
    val delivered: Boolean,
    val detail: String?,
)
