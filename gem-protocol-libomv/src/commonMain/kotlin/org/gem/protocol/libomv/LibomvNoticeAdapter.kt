package org.gem.protocol.libomv

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDraft
import org.gem.core.ports.NoticePort
import org.gem.protocol.libomv.runtime.ProtocolNoticeRuntime

class LibomvNoticeAdapter(
    internal val clientSession: LibomvClientSession,
    private val noticeRuntime: ProtocolNoticeRuntime? = null,
) : NoticePort {
    override fun sendGroupNotice(
        session: GemSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus =
        noticeRuntime?.sendGroupNotice(session, group, draft, attachment)
            ?: GroupSendStatus(
                group = group,
                state = GroupSendState.FAILED,
                detail = clientSession.unavailable(CoreFailureReason.NOTICE_SEND_FAILED).redactedMessage,
            )
}
