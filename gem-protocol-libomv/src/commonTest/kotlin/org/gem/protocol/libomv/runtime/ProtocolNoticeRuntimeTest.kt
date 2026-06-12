package org.gem.protocol.libomv.runtime

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.SessionId
import org.gem.core.domain.TargetSelectionResult
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.mapping.LibomvNoticeMapping
import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtocolNoticeRuntimeTest {
    @Test
    fun `plain notice builds source-derived group notice packet`() {
        val session = gemSession()
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(session, source).sendGroupNotice(
            session = session,
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.SENT, status.state)
        val identity = source.identities.single()
        assertEquals(AGENT_ID, identity.agentId)
        assertEquals(SESSION_ID, identity.sessionId)
        val packet = source.packets.single()
        assertEquals(AGENT_ID, packet.agentId)
        assertEquals(SESSION_ID, packet.sessionId)
        assertFalse(packet.fromGroup)
        assertEquals(GROUP_ID, packet.targetGroupId)
        assertEquals("Venue Host", packet.fromAgentName)
        assertEquals("Gig tonight|Doors at 8", packet.message)
        assertEquals(LibomvNoticeMapping.GROUP_NOTICE_DIALOG, packet.dialog)
        assertEquals(LibomvNoticeMapping.ONLINE, packet.offline)
        assertEquals(EXPECTED_INSTANT_MESSAGE_ID, packet.instantMessageId)
        assertEquals(LibomvNoticeMapping.PARENT_ESTATE_ID, packet.parentEstateId)
        assertEquals(LibomvNoticeMapping.TIMESTAMP, packet.timestamp)
        assertEquals(LibomvNoticeMapping.ZERO_ID, packet.regionId)
        assertContentEquals(ByteArray(0), packet.binaryBucket)
    }

    @Test
    fun `sent notice preserves redacted transport detail`() {
        val session = gemSession()
        val source = RecordingNoticeRuntimeSource(
            NoticeRuntimeResult.Sent("transportAck=passed; instantMessages=1"),
        )

        val status = runtime(session, source).sendGroupNotice(
            session = session,
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.SENT, status.state)
        assertEquals("transportAck=passed; instantMessages=1", status.detail)
    }

    @Test
    fun `landmark attachment serializes logged in agent owner and kind`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
            group = group(),
            draft = draft(),
            attachment = attachment(AttachmentKind.LANDMARK),
        )

        assertEquals(GroupSendState.SENT, status.state)
        val packet = source.packets.single()
        assertEquals(AttachmentKind.LANDMARK, packet.attachment?.kind)
        val bucket = packet.binaryBucket.decodeToString()
        assertEquals(metaboltAttachmentBucket(), bucket)
    }

    @Test
    fun `texture attachment serializes item owner and kind`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
            group = group(),
            draft = draft(),
            attachment = attachment(AttachmentKind.TEXTURE),
        )

        assertEquals(GroupSendState.SENT, status.state)
        val packet = source.packets.single()
        assertEquals(AttachmentKind.TEXTURE, packet.attachment?.kind)
        assertEquals(metaboltAttachmentBucket(), packet.binaryBucket.decodeToString())
    }

    @Test
    fun `non sendable group is skipped before source call`() {
        val source = RecordingNoticeRuntimeSource()

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
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
        val runtime = runtime(gemSession(SESSION_ID), source)

        val status = runtime.sendGroupNotice(
            session = gemSession(OTHER_SESSION_ID),
            group = group(),
            draft = draft(),
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("gem session mismatch", status.detail)
        assertFalse(status.detail.orEmpty().contains(SESSION_ID))
        assertFalse(status.detail.orEmpty().contains(OTHER_SESSION_ID))
        assertTrue(source.packets.isEmpty())
    }

    @Test
    fun `missing protocol agent identity fails before source call`() {
        val session = gemSession()
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

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
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

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
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

        val status = runtime(gemSession(), source).sendGroupNotice(
            session = gemSession(),
            group = group(),
            draft = invalidDraft,
            attachment = null,
        )

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice request invalid", status.detail)
        assertTrue(source.packets.isEmpty())
    }

    private fun runtime(
        session: GemSession,
        source: RecordingNoticeRuntimeSource,
    ): ProtocolNoticeRuntime = ProtocolNoticeRuntime(
        clientSession = activeSession(session),
        noticeSource = source,
    )

    private fun activeSession(session: GemSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
        agentName = "Venue Host",
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

    private fun metaboltAttachmentBucket(): String =
        "<llsd><map>" +
            "<key>item_id</key><uuid>$ITEM_ID</uuid>" +
            "<key>owner_id</key><uuid>$AGENT_ID</uuid>" +
            "</map></llsd>"

    private fun group(
        id: String = GROUP_ID,
        canSendNotices: Boolean = true,
    ): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName("Music Room"),
        canSendNotices = canSendNotices,
        acceptsNotices = true,
    )

    private fun gemSession(id: String = SESSION_ID): GemSession = GemSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private class RecordingNoticeRuntimeSource(
        private val result: NoticeRuntimeResult = NoticeRuntimeResult.Sent(),
    ) : NoticeRuntimeSource {
        val identities = mutableListOf<LibomvSessionIdentity>()
        val packets = mutableListOf<LibomvNoticePacket>()

        override fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult {
            identities += identity
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
