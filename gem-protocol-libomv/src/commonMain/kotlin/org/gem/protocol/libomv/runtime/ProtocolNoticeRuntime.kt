package org.gem.protocol.libomv.runtime

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDraft
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.LibomvSessionIdentityResult
import org.gem.protocol.libomv.mapping.LibomvNoticeMapping
import org.gem.protocol.libomv.mapping.LibomvNoticeMappingResult
import org.gem.protocol.libomv.mapping.LibomvNoticePacket

class ProtocolNoticeRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val noticeSource: NoticeRuntimeSource = NoticeRuntimeSource.unavailable(),
) {
    fun sendGroupNotice(
        session: GemSession,
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

        return when (val result = noticeSource.send(identity, packet)) {
            is NoticeRuntimeResult.Sent -> GroupSendStatus(group, GroupSendState.SENT, detail = result.redactedDetail)
            is NoticeRuntimeResult.Failed -> failed(group, redactedProtocolFailure(result.message))
        }
    }

    private fun failed(group: GroupMembership, failure: CoreFailure): GroupSendStatus =
        GroupSendStatus(group, GroupSendState.FAILED, detail = failure.redactedMessage)

    private fun failed(group: GroupMembership, message: String): GroupSendStatus =
        GroupSendStatus(group, GroupSendState.FAILED, detail = message)

    private fun redactedProtocolFailure(message: String): String =
        if (ID_PATTERN.containsMatchIn(message)) "notice send failed" else message

    private companion object {
        val ID_PATTERN: Regex =
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}

internal fun interface NoticeRuntimeSource {
    fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult

    companion object {
        fun unavailable(): NoticeRuntimeSource =
            NoticeRuntimeSource { _, _ -> NoticeRuntimeResult.Failed("notice runtime unavailable") }
    }
}

internal sealed interface NoticeRuntimeResult {
    data class Sent(val redactedDetail: String? = null) : NoticeRuntimeResult
    data class Failed(val message: String) : NoticeRuntimeResult
}
