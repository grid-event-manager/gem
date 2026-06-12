package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GemDelay
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDispatchResult
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.PacingPolicy
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.GroupListResult
import org.gem.core.testing.FakeClockPort
import org.gem.core.testing.FakeGroupPort
import org.gem.core.testing.FakeInventoryPort
import org.gem.core.testing.FakeNoticePort
import org.gem.core.testing.defaultSession
import org.gem.core.testing.failure

class NoticeReadinessFlowTest {
    @Test
    fun `target readiness uses group directory output and display names`() {
        val groups = venueGroups()
        val groupPort = FakeGroupPort(GroupListResult.Success(groups))
        val listedGroups = assertIs<GroupListResult.Success>(
            GroupDirectoryService(groupPort).currentGroups(defaultSession()),
        ).groups
        val targetService = TargetSelectionService()
        val emptyTargets = targetService.emptyTargetSet(listedGroups)

        val missing = assertIs<TargetSelectionResult.NoSuchGroup>(
            targetService.addTargetByDisplayName(emptyTargets, "Missing Group"),
        )
        val selected = assertIs<TargetSelectionResult.Changed>(
            targetService.addAllSendable(missing.targetSet),
        ).targetSet
        val cleared = assertIs<TargetSelectionResult.Changed>(
            targetService.removeAll(selected),
        ).targetSet

        assertEquals(listOf("Venue Hosts", "Event Notices"), selected.selectedGroups.map { it.displayName.value })
        assertTrue(cleared.isEmpty())
        assertEquals(listOf(defaultSession()), groupPort.sessions)
    }

    @Test
    fun `pasted subject message and selected attachment create a valid draft without asset read`() {
        val selected = selectedTargets()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("venue-landmark"))
        val draft = NoticeDraftService().createDraft(
            subject = "Tonight at 8",
            message = "Doors open at 7:45. Landmark attached.",
            targetSet = selected,
            attachments = listOf(request),
        )

        assertEquals("Doors open at 7:45. Landmark attached.", draft.message)
        assertEquals(listOf(request), draft.attachments)
        assertEquals(NoticeDraftValidation.Valid, NoticeDraftService().validateForSend(draft))
    }

    @Test
    fun `existing landmark attachment resolves through attachment service`() {
        val inventoryPort = FakeInventoryPort()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("venue-landmark"))

        val result = assertIs<AttachmentResolutionResult.Resolved>(
            AttachmentService(inventoryPort).resolveAttachment(defaultSession(), request),
        )

        assertEquals(listOf(request), inventoryPort.existingRequests)
        assertEquals(AttachmentKind.LANDMARK, result.attachment.kind)
    }

    @Test
    fun `attachment resolution failure prevents attachment readiness claim`() {
        val inventoryPort = FakeInventoryPort(
            existingResult = AttachmentResolutionResult.Failed(
                failure(CoreFailureReason.ATTACHMENT_NOT_FOUND, "landmark unavailable"),
            ),
        )
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("missing-landmark"))

        val result = assertIs<AttachmentResolutionResult.Failed>(
            AttachmentService(inventoryPort).resolveAttachment(defaultSession(), request),
        )

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, result.failure.reason)
        assertEquals(listOf(request), inventoryPort.existingRequests)
    }

    @Test
    fun `notice readiness dispatches only through notice dispatch service`() {
        val noticePort = FakeNoticePort()
        val clockPort = FakeClockPort()
        val inventoryPort = FakeInventoryPort()
        val targetSet = selectedTargets()
        val draft = NoticeDraftService().createDraft(
            subject = "Tonight at 8",
            message = "Operator pasted this text from an external notecard viewer.",
            targetSet = targetSet,
            attachments = listOf(ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("venue-landmark"))),
        )
        val attachment = assertIs<AttachmentResolutionResult.Resolved>(
            AttachmentService(inventoryPort).resolveAttachment(
                defaultSession(),
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("venue-landmark")),
            ),
        ).attachment

        val result = assertIs<NoticeDispatchResult.Sent>(
            noticeDispatchService(noticePort, clockPort).dispatch(
                session = defaultSession(),
                draft = draft,
                pacingPolicy = PacingPolicy(GemDelay.ofMilliseconds(25)),
                attachment = attachment,
            ),
        )

        assertEquals(listOf(GroupSendState.SENT, GroupSendState.SENT), result.result.statuses.map { it.state })
        assertEquals(listOf(GemDelay.ofMilliseconds(25)), clockPort.pauses)
        assertEquals(targetSet.selectedGroups, noticePort.calls.map { it.group })
        assertEquals(listOf(attachment, attachment), noticePort.calls.map { it.attachment })
    }

    private fun selectedTargets() = assertIs<TargetSelectionResult.Changed>(
        TargetSelectionService().addAllSendable(
            TargetSelectionService().emptyTargetSet(venueGroups()),
        ),
    ).targetSet

    private fun noticeDispatchService(
        noticePort: FakeNoticePort,
        clockPort: FakeClockPort,
    ): NoticeDispatchService =
        NoticeDispatchService(
            noticePort = noticePort,
            clockPort = clockPort,
        )

    private fun venueGroups(): List<GroupMembership> = listOf(
        group("venue-hosts", "Venue Hosts", canSendNotices = true),
        group("event-notices", "Event Notices", canSendNotices = true),
        group("chat-only", "Chat Only", canSendNotices = false),
    )

    private fun group(
        id: String,
        displayName: String,
        canSendNotices: Boolean,
    ): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = canSendNotices,
        acceptsNotices = null,
    )

}
