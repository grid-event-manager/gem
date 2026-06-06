package org.hostess.protocol.libomv

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.ports.NoticePort
import org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime

class LibomvNoticeAdapter(
    internal val clientSession: LibomvClientSession,
    private val noticeRuntime: ProtocolNoticeRuntime? = null,
) : NoticePort {
    override fun sendGroupNotice(
        session: HostessSession,
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
