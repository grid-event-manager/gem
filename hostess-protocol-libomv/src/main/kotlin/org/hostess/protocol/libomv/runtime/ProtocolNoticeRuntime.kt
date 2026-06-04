package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentityResult
import org.hostess.protocol.libomv.mapping.LibomvNoticeMapping
import org.hostess.protocol.libomv.mapping.LibomvNoticeMappingResult
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket

class ProtocolNoticeRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val noticeSource: NoticeRuntimeSource = NoticeRuntimeSource.unavailable(),
) {
    fun sendGroupNotice(
        session: HostessSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure ->
                return failed(group, result.failure.copy(reason = CoreFailureReason.NOTICE_SEND_FAILED))
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        if (!group.canSendNotices) {
            return GroupSendStatus(group, GroupSendState.SKIPPED, detail = "group cannot send notices")
        }
        val packet = when (
            val mapped = LibomvNoticeMapping.noticePacket(identity, session, group, draft, attachment)
        ) {
            LibomvNoticeMappingResult.Failure -> return failed(group, "notice request invalid")
            is LibomvNoticeMappingResult.Success -> mapped.packet
        }

        return when (val result = noticeSource.send(packet)) {
            NoticeRuntimeResult.Sent -> GroupSendStatus(group, GroupSendState.SENT)
            is NoticeRuntimeResult.Failed -> failed(group, redactedProtocolFailure(result.message))
        }
    }

    private fun failed(group: GroupMembership, failure: CoreFailure): GroupSendStatus =
        GroupSendStatus(group, GroupSendState.FAILED, detail = failure.redactedMessage)

    private fun failed(group: GroupMembership, message: String): GroupSendStatus =
        GroupSendStatus(group, GroupSendState.FAILED, detail = message)

    private fun redactedProtocolFailure(message: String): String =
        if (UUID_PATTERN.containsMatchIn(message)) "notice send failed" else message

    private companion object {
        val UUID_PATTERN: Regex =
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}

internal fun interface NoticeRuntimeSource {
    fun send(packet: LibomvNoticePacket): NoticeRuntimeResult

    companion object {
        fun unavailable(): NoticeRuntimeSource =
            NoticeRuntimeSource { NoticeRuntimeResult.Failed("notice runtime unavailable") }
    }
}

internal sealed interface NoticeRuntimeResult {
    data object Sent : NoticeRuntimeResult
    data class Failed(val message: String) : NoticeRuntimeResult
}
