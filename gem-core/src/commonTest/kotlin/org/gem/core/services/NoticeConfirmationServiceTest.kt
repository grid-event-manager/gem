package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupNoticeConfirmationState
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeConfirmationResult
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeSendPlan
import org.gem.core.domain.NoticeSendResult
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveEntry
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.testing.defaultPresenceProof
import org.gem.core.testing.defaultSession

class NoticeConfirmationServiceTest {
    @Test
    fun `confirms sent group when archive contains exact subject without requiring attachment`() {
        val group = group("owks", "Owks")
        val port = ScriptedGroupPort(
            archiveResults = mapOf(
                group.groupId to GroupNoticeArchiveResult.Success(
                    group = group,
                    entries = listOf(archiveEntry(subject = "Tonight", hasAttachment = false)),
                ),
            ),
        )

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                statuses = listOf(GroupSendStatus(group, GroupSendState.SENT)),
            ),
        )

        assertTrue(result.allConfirmed)
        assertEquals(listOf(GroupNoticeConfirmationState.CONFIRMED), result.statuses.map { it.state })
        assertEquals(listOf(group), port.archiveRequests)
    }

    @Test
    fun `requires attachment echo only when draft has an attachment`() {
        val group = group("owks", "Owks")
        val port = ScriptedGroupPort(
            archiveResults = mapOf(
                group.groupId to GroupNoticeArchiveResult.Success(
                    group = group,
                    entries = listOf(archiveEntry(subject = "Tonight", hasAttachment = false)),
                ),
            ),
        )

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                hasAttachment = true,
                statuses = listOf(GroupSendStatus(group, GroupSendState.SENT)),
            ),
        )

        assertFalse(result.allConfirmed)
        assertEquals(GroupNoticeConfirmationState.UNCONFIRMED, result.statuses.single().state)
        assertEquals("notice archive proof_gap subject_or_attachment_not_found; attempts=4", result.statuses.single().detail)
    }

    @Test
    fun `reports unconfirmed when archive has no exact subject match`() {
        val group = group("minx", "m!nx")
        val port = ScriptedGroupPort(
            archiveResults = mapOf(
                group.groupId to GroupNoticeArchiveResult.Success(
                    group = group,
                    entries = listOf(archiveEntry(subject = "Tomorrow", hasAttachment = true)),
                ),
            ),
        )

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                statuses = listOf(GroupSendStatus(group, GroupSendState.SENT)),
            ),
        )

        assertFalse(result.allConfirmed)
        assertEquals(GroupNoticeConfirmationState.UNCONFIRMED, result.statuses.single().state)
        assertEquals("notice archive proof_gap subject_or_attachment_not_found; attempts=4", result.statuses.single().detail)
    }

    @Test
    fun `skips archive read for transport failures`() {
        val group = group("minx", "m!nx")
        val port = ScriptedGroupPort()

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                statuses = listOf(GroupSendStatus(group, GroupSendState.FAILED, "ack timeout")),
            ),
        )

        assertFalse(result.allConfirmed)
        assertEquals(emptyList(), port.archiveRequests)
        assertEquals(GroupNoticeConfirmationState.SKIPPED, result.statuses.single().state)
        assertEquals("ack timeout", result.statuses.single().detail)
    }

    @Test
    fun `reports archive failures with redacted detail`() {
        val group = group("owks", "Owks")
        val port = ScriptedGroupPort(
            archiveResults = mapOf(
                group.groupId to GroupNoticeArchiveResult.Failure(
                    group = group,
                    failure = CoreFailure(
                        reason = CoreFailureReason.GROUP_LIST_FAILED,
                        redactedMessage = "archive failed session_id=secret-value",
                    ),
                ),
            ),
        )

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                statuses = listOf(GroupSendStatus(group, GroupSendState.SENT)),
            ),
        )

        assertFalse(result.allConfirmed)
        assertEquals(GroupNoticeConfirmationState.FAILED, result.statuses.single().state)
        assertEquals("archive failed session_id=[redacted]; attempts=4", result.statuses.single().detail)
    }

    @Test
    fun `retries archive failures before reporting unconfirmed notice`() {
        val group = group("minx", "m!nx")
        val port = ScriptedGroupPort(
            archiveSequences = mapOf(
                group.groupId to listOf(
                    GroupNoticeArchiveResult.Failure(
                        group = group,
                        failure = CoreFailure(
                            reason = CoreFailureReason.GROUP_LIST_FAILED,
                            redactedMessage = "notice archive proof_gap reply_timeout",
                        ),
                    ),
                    GroupNoticeArchiveResult.Success(
                        group = group,
                        entries = listOf(archiveEntry(subject = "Tonight", hasAttachment = false)),
                    ),
                ),
            ),
        )

        val result = service(port).confirmArchive(
            session = defaultSession(),
            sendResult = sendResult(
                subject = "Tonight",
                groups = listOf(group),
                statuses = listOf(GroupSendStatus(group, GroupSendState.SENT)),
            ),
        )

        assertTrue(result.allConfirmed)
        assertEquals(listOf(GroupNoticeConfirmationState.CONFIRMED), result.statuses.map { it.state })
        assertEquals(listOf(group, group), port.archiveRequests)
    }

    @Test
    fun `empty confirmation result is never all confirmed`() {
        assertFalse(NoticeConfirmationResult(emptyList()).allConfirmed)
    }

    private fun service(port: GroupPort): NoticeConfirmationService =
        NoticeConfirmationService(GroupDirectoryService(port))

    private fun sendResult(
        subject: String,
        groups: List<GroupMembership>,
        statuses: List<GroupSendStatus>,
        hasAttachment: Boolean = false,
    ): NoticeSendResult {
        val targetSet = assertChanged(GroupTargetSet.from(groups).addAllSendable())
        val attachments = if (hasAttachment) {
            listOf(ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("welcome-area")))
        } else {
            emptyList()
        }
        val draft = NoticeDraft(
            subject = subject,
            message = "Doors at 9",
            targetSet = targetSet,
            attachments = attachments,
        )
        return NoticeSendResult(NoticeSendPlan(draft), statuses)
    }

    private fun assertChanged(result: TargetSelectionResult): GroupTargetSet =
        (result as TargetSelectionResult.Changed).targetSet

    private fun group(id: String, displayName: String): GroupMembership =
        GroupMembership.fromValues(
            groupId = id,
            displayName = displayName,
            canSendNotices = true,
            acceptsNotices = true,
        )

    private fun archiveEntry(
        subject: String,
        hasAttachment: Boolean,
    ): GroupNoticeArchiveEntry =
        GroupNoticeArchiveEntry(
            subject = subject,
            fromName = "proof-account",
            timestamp = 1_717_000_000L,
            hasAttachment = hasAttachment,
            assetType = 3,
        )

    private class ScriptedGroupPort(
        private val archiveResults: Map<GroupId, GroupNoticeArchiveResult> = emptyMap(),
        archiveSequences: Map<GroupId, List<GroupNoticeArchiveResult>> = emptyMap(),
    ) : GroupPort {
        private val archiveSequences = archiveSequences
            .mapValues { (_, results) -> results.toMutableList() }
            .toMutableMap()
        val archiveRequests = mutableListOf<GroupMembership>()

        override fun currentGroups(session: org.gem.core.domain.GemSession): GroupListResult =
            GroupListResult.Success(emptyList())

        override fun simulatorPresence(
            session: org.gem.core.domain.GemSession,
        ): SimulatorPresenceProofResult =
            SimulatorPresenceProofResult.Success(defaultPresenceProof())

        override fun noticeArchive(
            session: org.gem.core.domain.GemSession,
            group: GroupMembership,
        ): GroupNoticeArchiveResult {
            archiveRequests += group
            archiveSequences[group.groupId]
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it.removeAt(0) }
            return archiveResults[group.groupId] ?: GroupNoticeArchiveResult.Success(group, emptyList())
        }
    }
}
