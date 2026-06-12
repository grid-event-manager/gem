package org.gem.core.ports

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDraft

interface NoticePort {
    fun sendGroupNotice(
        session: GemSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus
}
