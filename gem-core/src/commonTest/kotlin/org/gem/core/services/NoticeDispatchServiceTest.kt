package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDispatchResult
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeDraftInvalidReason
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.PacingPolicy
import org.gem.core.domain.SessionId
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.testing.FakeClockPort
import org.gem.core.testing.FakeNoticePort

class NoticeDispatchServiceTest {
    @Test
    fun `dispatch calls notice port once per selected group and returns send statuses`() {
        val events = mutableListOf<String>()
        val noticePort = FakeNoticePort(
            events = events,
            statesByGroup = mapOf(
                GroupId("music") to GroupSendState.SENT,
                GroupId("gallery") to GroupSendState.FAILED,
            ),
        )
        val service = service(noticePort, FakeClockPort(events))
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
            attachments = listOf(existingLandmarkAttachment()),
        )

        val result = assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(
                session = session(),
                draft = draft,
                pacingPolicy = PacingPolicy(GemDelay.ofSeconds(5)),
            ),
        ).result

        assertEquals(listOf("send:music", "pause:5000ms", "send:gallery"), events)
        assertEquals(listOf(GroupSendState.SENT, GroupSendState.FAILED), result.statuses.map { it.state })
        assertEquals(listOf("music", "gallery"), noticePort.calls.map { it.group.groupId.value })
    }

    @Test
    fun `dispatch rejects invalid draft without calling notice port`() {
        val events = mutableListOf<String>()
        val noticePort = FakeNoticePort(events)
        val service = service(noticePort, FakeClockPort(events))
        val invalidDraft = NoticeDraft(
            subject = "",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.Rejected>(
            service.dispatch(session(), invalidDraft),
        )

        val validation = assertIs<NoticeDraftValidation.Invalid>(result.validation)
        assertTrue(NoticeDraftInvalidReason.BLANK_SUBJECT in validation.reasons)
        assertTrue(events.isEmpty())
        assertTrue(noticePort.calls.isEmpty())
    }

    @Test
    fun `dispatch sends plain notice when attachment is absent`() {
        val events = mutableListOf<String>()
        val noticePort = FakeNoticePort(events)
        val service = service(noticePort, FakeClockPort(events))
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(session(), draft),
        )

        assertEquals(2, result.result.statuses.size)
        assertEquals(listOf(null, null), noticePort.calls.map { it.attachment })
    }

    @Test
    fun `dispatch forwards resolved attachment to each selected group`() {
        val noticePort = FakeNoticePort()
        val service = service(noticePort, FakeClockPort())
        val attachment = AttachmentRef(
            attachmentId = InventoryItemId("landmark-item"),
            ownerId = AttachmentOwnerId("owner"),
            kind = AttachmentKind.LANDMARK,
        )
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
            attachments = listOf(existingLandmarkAttachment()),
        )

        assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(
                session = session(),
                draft = draft,
                attachment = attachment,
            ),
        )

        assertEquals(listOf(attachment, attachment), noticePort.calls.map { it.attachment })
    }

    private fun selectedTargets() = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(
            listOf(
                group("music", "Music Room"),
                group("gallery", "Gallery"),
            ),
        ).addAllSendable(),
    ).targetSet

    private fun group(id: String, displayName: String): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = true,
        acceptsNotices = null,
    )

    private fun session(): GemSession = GemSession(
        sessionId = SessionId("session"),
        accountLabel = AccountLabel("proof-account"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun service(
        noticePort: FakeNoticePort,
        clockPort: FakeClockPort,
    ): NoticeDispatchService = NoticeDispatchService(
        noticePort = noticePort,
        clockPort = clockPort,
    )

    private fun existingLandmarkAttachment(): ExistingInventoryAttachment =
        ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
}
