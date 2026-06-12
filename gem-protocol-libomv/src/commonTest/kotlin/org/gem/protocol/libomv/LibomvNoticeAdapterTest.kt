package org.gem.protocol.libomv

import org.gem.core.domain.GemInstant
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.SessionId
import org.gem.core.domain.TargetSelectionResult
import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.runtime.NoticeRuntimeResult
import org.gem.protocol.libomv.runtime.NoticeRuntimeSource
import org.gem.protocol.libomv.runtime.ProtocolNoticeRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvNoticeAdapterTest {
    @Test
    fun `notice adapter routes through protocol runtime`() {
        val session = gemSession()
        val source = RecordingNoticeRuntimeSource()
        val clientSession = activeSession(session)
        val adapter = LibomvNoticeAdapter(
            clientSession = clientSession,
            noticeRuntime = ProtocolNoticeRuntime(clientSession, source),
        )

        val status = adapter.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.SENT, status.state)
        assertEquals(AGENT_ID, source.identities.single().agentId)
        assertEquals("Gig tonight|Doors at 8", source.packets.single().message)
    }

    @Test
    fun `notice adapter fallback still fails closed without runtime`() {
        val session = gemSession()
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

    private fun gemSession(): GemSession = GemSession(
        sessionId = SessionId(SESSION_ID),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun activeSession(session: GemSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )

    private class RecordingNoticeRuntimeSource : NoticeRuntimeSource {
        val identities = mutableListOf<LibomvSessionIdentity>()
        val packets = mutableListOf<LibomvNoticePacket>()

        override fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult {
            identities += identity
            packets += packet
            return NoticeRuntimeResult.Sent()
        }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
    }
}
