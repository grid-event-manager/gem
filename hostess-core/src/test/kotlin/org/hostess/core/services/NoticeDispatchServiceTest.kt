package org.hostess.core.services

import java.time.Duration
import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.NoticePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeDispatchServiceTest {
    @Test
    fun `dispatch calls notice port once per selected group and applies pacing between groups`() {
        val events = mutableListOf<String>()
        val noticePort = RecordingNoticePort(
            events = events,
            statesByGroup = mapOf(
                GroupId("music") to GroupSendState.SENT,
                GroupId("gallery") to GroupSendState.FAILED,
            ),
        )
        val service = NoticeDispatchService(noticePort, RecordingClockPort(events))
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(
                session = session(),
                draft = draft,
                pacingPolicy = PacingPolicy(Duration.ofSeconds(5)),
            ),
        ).result

        assertEquals(listOf("send:music", "pause:PT5S", "send:gallery"), events)
        assertEquals(listOf(GroupSendState.SENT, GroupSendState.FAILED), result.statuses.map { it.state })
    }

    @Test
    fun `dispatch rejects invalid draft without calling notice port`() {
        val events = mutableListOf<String>()
        val service = NoticeDispatchService(RecordingNoticePort(events), RecordingClockPort(events))
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
    }

    private class RecordingNoticePort(
        private val events: MutableList<String>,
        private val statesByGroup: Map<GroupId, GroupSendState> = emptyMap(),
    ) : NoticePort {
        override fun sendGroupNotice(
            session: HostessSession,
            group: GroupMembership,
            draft: NoticeDraft,
            attachment: AttachmentRef?,
        ): GroupSendStatus {
            events += "send:${group.groupId.value}"
            return GroupSendStatus(
                group = group,
                state = statesByGroup[group.groupId] ?: GroupSendState.SENT,
            )
        }
    }

    private class RecordingClockPort(
        private val events: MutableList<String>,
    ) : ClockPort {
        override fun now(): Instant = Instant.EPOCH

        override fun pause(duration: Duration) {
            events += "pause:$duration"
        }
    }

    private fun selectedTargets() = assertIs<TargetSelectionResult.Changed>(
        org.hostess.core.domain.GroupTargetSet.from(
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

    private fun session(): HostessSession = HostessSession(
        sessionId = SessionId("session"),
        accountLabel = AccountLabel("proof-account"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )
}
