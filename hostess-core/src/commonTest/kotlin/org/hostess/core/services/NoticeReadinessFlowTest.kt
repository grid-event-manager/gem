package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.NoticeRecipientCount
import org.hostess.core.domain.NoticeRecipientEstimate
import org.hostess.core.domain.NoticeRecipientEstimateSource
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.NoticeComplianceLedgerPort
import org.hostess.core.testing.FakeClockPort
import org.hostess.core.testing.FakeGroupPort
import org.hostess.core.testing.FakeInventoryPort
import org.hostess.core.testing.FakeNoticePort
import org.hostess.core.testing.defaultSession
import org.hostess.core.testing.failure

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
    fun `pasted subject and message create a valid draft without asset read`() {
        val selected = selectedTargets()
        val draft = NoticeDraftService().createDraft(
            subject = "Tonight at 8",
            message = "Doors open at 7:45. Landmark attached.",
            targetSet = selected,
        )

        assertEquals("Doors open at 7:45. Landmark attached.", draft.message)
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
                compliance = complianceRequest(),
                pacingPolicy = PacingPolicy(HostessDelay.ofMilliseconds(25)),
                attachment = attachment,
            ),
        )

        assertEquals(listOf(GroupSendState.SENT, GroupSendState.SENT), result.result.statuses.map { it.state })
        assertEquals("allowed", result.complianceReceipt.reasonCode)
        assertEquals(listOf(HostessDelay.ofMilliseconds(25)), clockPort.pauses)
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
            noticeComplianceService = NoticeComplianceService(
                policy = NoticeCompliancePolicy(),
                ledger = PassingNoticeComplianceLedger,
                clock = NoticeComplianceClock { NoticeDeliveryDay("2026-06-06") },
            ),
        )

    private fun complianceRequest(): NoticeComplianceRequest = NoticeComplianceRequest(
        operatorLabel = OperatorLabel("operator"),
        recipientEstimates = listOf(
            estimate("Venue Hosts", 100),
            estimate("Event Notices", 100),
        ),
    )

    private fun estimate(displayName: String, count: Long): NoticeRecipientEstimate =
        NoticeRecipientEstimate(
            displayName = GroupDisplayName(displayName),
            recipientCount = NoticeRecipientCount(count),
            source = NoticeRecipientEstimateSource.OPERATOR_ACKNOWLEDGED,
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

    private object PassingNoticeComplianceLedger : NoticeComplianceLedgerPort {
        override fun snapshot(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
            NoticeComplianceLedgerResult.Success(ledgerSnapshot(operatorLabel, deliveryDay))

        override fun reserve(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            projected: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
            NoticeComplianceLedgerResult.Success(ledgerSnapshot(operatorLabel, deliveryDay, reserved = projected.value))

        override fun recordSendResult(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            reservedProjection: NoticeDeliveryCount,
            delivered: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
            NoticeComplianceLedgerResult.Success(ledgerSnapshot(operatorLabel, deliveryDay, sent = delivered.value))

        private fun ledgerSnapshot(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            reserved: Long = 0,
            sent: Long = 0,
        ): NoticeDeliveryLedgerSnapshot = NoticeDeliveryLedgerSnapshot(
            operatorLabel = operatorLabel,
            deliveryDay = deliveryDay,
            reservedDeliveryCount = NoticeDeliveryCount(reserved),
            recordedSentDeliveryCount = NoticeDeliveryCount(sent),
        )
    }
}
