package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvNoticeMapping
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtocolNoticeRuntimeTest {
    @Test
    fun `plain notice builds source-derived group notice packet`() {
        val session = hostessSession()
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(session, source).sendGroupNotice(
            session = session,
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.SENT, status.state)
        val packet = source.packets.single()
        assertEquals(AGENT_ID, packet.agentId)
        assertEquals(SESSION_ID, packet.sessionId)
        assertFalse(packet.fromGroup)
        assertEquals(GROUP_ID, packet.targetGroupId)
        assertEquals("venue-proof", packet.fromAgentName)
        assertEquals("Gig tonight|Doors at 8", packet.message)
        assertEquals(LibomvNoticeMapping.GROUP_NOTICE_DIALOG, packet.dialog)
        assertEquals(LibomvNoticeMapping.ONLINE, packet.offline)
        assertEquals(EXPECTED_INSTANT_MESSAGE_ID, packet.instantMessageId)
        assertEquals(LibomvNoticeMapping.PARENT_ESTATE_ID, packet.parentEstateId)
        assertEquals(LibomvNoticeMapping.TIMESTAMP, packet.timestamp)
        assertEquals(LibomvNoticeMapping.ZERO_UUID, packet.regionId)
        assertContentEquals(ByteArray(0), packet.binaryBucket)
    }

    @Test
    fun `landmark attachment serializes item owner and kind`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(),
            draft = draft(),
            attachment = attachment(AttachmentKind.LANDMARK),
        )

        assertEquals(GroupSendState.SENT, status.state)
        val packet = source.packets.single()
        assertEquals(AttachmentKind.LANDMARK, packet.attachment?.kind)
        val bucket = packet.binaryBucket.decodeToString()
        assertTrue(bucket.contains("<key>item_id</key><uuid>$ITEM_ID</uuid>"))
        assertTrue(bucket.contains("<key>owner_id</key><uuid>$OWNER_ID</uuid>"))
    }

    @Test
    fun `texture attachment serializes item owner and kind`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(),
            draft = draft(),
            attachment = attachment(AttachmentKind.TEXTURE),
        )

        assertEquals(GroupSendState.SENT, status.state)
        val packet = source.packets.single()
        assertEquals(AttachmentKind.TEXTURE, packet.attachment?.kind)
        assertTrue(packet.binaryBucket.decodeToString().contains(ITEM_ID))
    }

    @Test
    fun `non sendable group is skipped before source call`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(canSendNotices = false),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.SKIPPED, status.state)
        assertEquals("group cannot send notices", status.detail)
        assertTrue(source.packets.isEmpty())
    }

    @Test
    fun `session mismatch fails without calling source or leaking IDs`() {
        val source = RecordingNoticeRuntimeSource()
        val runtime = runtime(hostessSession(SESSION_ID), source)

        val status = runtime.sendGroupNotice(
            session = hostessSession(OTHER_SESSION_ID),
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("hostess session mismatch", status.detail)
        assertFalse(status.detail.orEmpty().contains(SESSION_ID))
        assertFalse(status.detail.orEmpty().contains(OTHER_SESSION_ID))
        assertTrue(source.packets.isEmpty())
    }

    @Test
    fun `missing protocol agent identity fails before source call`() {
        val session = hostessSession()
        val source = RecordingNoticeRuntimeSource()
        val runtime = ProtocolNoticeRuntime(
            clientSession = LibomvClientSession.active(session),
            noticeSource = source,
        )

        val status = runtime.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("protocol agent identity unavailable", status.detail)
        assertTrue(source.packets.isEmpty())
    }

    @Test
    fun `invalid notice group uuid fails before source call`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(id = "not-a-uuid"),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice request invalid", status.detail)
        assertTrue(source.packets.isEmpty())
    }

    @Test
    fun `protocol failure is redacted and does not leak group UUID`() {
        val source = RecordingNoticeRuntimeSource(
            result = NoticeRuntimeResult.Failed("packet rejected for $GROUP_ID"),
        )

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice send failed", status.detail)
        assertFalse(status.detail.orEmpty().contains(GROUP_ID))
    }

    @Test
    fun `multi attachment drafts are rejected before source call`() {
        val source = RecordingNoticeRuntimeSource()
        val invalidDraft = draft(
            attachments = listOf(
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId(ITEM_ID)),
                ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId(ITEM_ID)),
            ),
        )

        val status = runtime(hostessSession(), source).sendGroupNotice(
            session = hostessSession(),
            group = group(),
            draft = invalidDraft,
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice request invalid", status.detail)
        assertTrue(source.packets.isEmpty())
    }

    private fun runtime(
        session: HostessSession,
        source: RecordingNoticeRuntimeSource,
    ): ProtocolNoticeRuntime = ProtocolNoticeRuntime(
        clientSession = activeSession(session),
        noticeSource = source,
    )

    private fun activeSession(session: HostessSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )

    private fun draft(
        subject: String = "Gig tonight",
        message: String = "Doors at 8",
        attachments: List<ExistingInventoryAttachment> = emptyList(),
    ): NoticeDraft = NoticeDraft(
        subject = subject,
        message = message,
        targetSet = selectedTargets(group()),
        attachments = attachments,
    )

    private fun selectedTargets(group: GroupMembership) = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(listOf(group)).addAllSendable(),
    ).targetSet

    private fun attachment(kind: AttachmentKind): AttachmentRef = AttachmentRef(
        attachmentId = InventoryItemId(ITEM_ID),
        ownerId = AttachmentOwnerId(OWNER_ID),
        kind = kind,
    )

    private fun group(
        id: String = GROUP_ID,
        canSendNotices: Boolean = true,
    ): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName("Music Room"),
        canSendNotices = canSendNotices,
        acceptsNotices = true,
    )

    private fun hostessSession(id: String = SESSION_ID): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private class RecordingNoticeRuntimeSource(
        private val result: NoticeRuntimeResult = NoticeRuntimeResult.Sent,
    ) : NoticeRuntimeSource {
        val packets = mutableListOf<LibomvNoticePacket>()

        override fun send(packet: LibomvNoticePacket): NoticeRuntimeResult {
            packets += packet
            return result
        }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val OTHER_SESSION_ID = "44444444-4444-4444-4444-444444444444"
        const val ITEM_ID = "55555555-5555-5555-5555-555555555555"
        const val OWNER_ID = "66666666-6666-6666-6666-666666666666"
        const val EXPECTED_INSTANT_MESSAGE_ID = "22222222-2222-2222-2222-222222222222"
    }
}
