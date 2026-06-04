package org.hostess.core.ports

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft

interface NoticePort {
    fun sendGroupNotice(
        session: HostessSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus
}
