package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.runtime.NoticeRuntimeResult
import org.hostess.protocol.libomv.runtime.NoticeRuntimeSource
import org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvNoticeAdapterTest {
    @Test
    fun `notice adapter routes through protocol runtime`() {
        val session = hostessSession()
        val source = RecordingNoticeRuntimeSource()
        val clientSession = LibomvClientSession.active(session, agentId = AGENT_ID)
        val adapter = LibomvNoticeAdapter(
            clientSession = clientSession,
            noticeRuntime = ProtocolNoticeRuntime(clientSession, source),
        )

        val status = adapter.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.SENT, status.state)
        assertEquals("Gig tonight|Doors at 8", source.packets.single().message)
    }

    @Test
    fun `notice adapter fallback still fails closed without runtime`() {
        val session = hostessSession()
        val adapter = LibomvNoticeAdapter(clientSession = LibomvClientSession.active(session))

        val status = adapter.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals(
            "protocol runtime unavailable",
            status.detail,
        )
    }

    private fun draft(): NoticeDraft = NoticeDraft(
        subject = "Gig tonight",
        message = "Doors at 8",
        targetSet = assertIs<TargetSelectionResult.Changed>(
            GroupTargetSet.from(listOf(group())).addAllSendable(),
        ).targetSet,
    )

    private fun group(): GroupMembership = GroupMembership(
        groupId = GroupId(GROUP_ID),
        displayName = GroupDisplayName("Music Room"),
        canSendNotices = true,
        acceptsNotices = true,
    )

    private fun hostessSession(): HostessSession = HostessSession(
        sessionId = SessionId(SESSION_ID),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )

    private class RecordingNoticeRuntimeSource : NoticeRuntimeSource {
        val packets = mutableListOf<LibomvNoticePacket>()

        override fun send(packet: LibomvNoticePacket): NoticeRuntimeResult {
            packets += packet
            return NoticeRuntimeResult.Sent
        }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
    }
}
