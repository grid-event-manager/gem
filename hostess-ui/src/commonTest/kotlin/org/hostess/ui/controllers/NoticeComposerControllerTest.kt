package org.hostess.ui.controllers

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupSendState
import org.hostess.ui.testing.FakeGroupFixtures
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeNoticeRecorder
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeComposerControllerTest {
    @Test
    fun subjectAndUnicodeBodyRoundTripThroughCoreDraftProjection() {
        val body = "Tonight at 9 \uD83C\uDFA7"
        val controller = NoticeComposerController(FakeHostessUiRuntime.ready())
            .updateSubject("Event tonight")
            .updateBody(body)

        assertEquals("Event tonight", controller.state.subject)
        assertEquals(body, controller.state.body)
        assertEquals(body.length, controller.state.charCount)
        assertFalse(controller.state.draftValid)
        assertTrue(NoticeDraftInvalidReason.EMPTY_TARGET_SET in controller.state.draftInvalidReasons)
        assertFalse(NoticeDraftInvalidReason.BLANK_MESSAGE in controller.state.draftInvalidReasons)
    }

    @Test
    fun blankBodyProjectsCoreBlankMessageReason() {
        val controller = NoticeComposerController(FakeHostessUiRuntime.ready())
            .updateSubject("Event tonight")
            .updateBody("")

        assertTrue(NoticeDraftInvalidReason.BLANK_MESSAGE in controller.state.draftInvalidReasons)
    }

    @Test
    fun sendNoticesProjectsSendingAndSentFeedback() {
        val recorder = org.hostess.ui.testing.FakeNoticeRecorder()
        val runtime = FakeHostessUiRuntime.ready(
            groups = FakeGroupFixtures.mixedGroups(),
            noticeRecorder = recorder,
        )
        val targets = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups().selectAllGroupsMode()
        val ready = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        )
            .updateSubject("Event tonight")
            .updateBody("Doors at 9")
            .updateTargetSet(targets.targetSet)
        val sending = ready.beginSend()
        val sent = sending.sendNotices()

        assertTrue(sending.state.sendFooterState.sending)
        assertEquals(HostessTextKey.SendingNotices, sending.state.sendFooterState.statusTextKey)
        assertEquals(HostessTextKey.NoticesSent, sent.state.sendFooterState.statusTextKey)
        assertFalse(sent.state.sendFooterState.sending)
        assertEquals(2, recorder.sendCallCount)
    }

    @Test
    fun sendNoticesProjectsUnconfirmedFeedbackWhenArchiveProofMisses() {
        val runtime = FakeHostessUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeArchiveEntriesByGroupId = emptyMap(),
        )
        val targets = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups().selectAllGroupsMode()
        val sent = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        )
            .updateSubject("Event tonight")
            .updateBody("Doors at 9")
            .updateTargetSet(targets.targetSet)
            .sendNotices()

        assertEquals(HostessTextKey.SomeNoticesUnconfirmed, sent.state.sendFooterState.statusTextKey)
        assertEquals(
            "Owks: notice archive proof_gap subject_or_attachment_not_found | " +
                "m!nx: notice archive proof_gap subject_or_attachment_not_found",
            sent.state.sendFooterState.detailText,
        )
    }

    @Test
    fun sendNoticesProjectsArchiveFailureWithoutHidingBehindSuccess() {
        val runtime = FakeHostessUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeArchiveFailuresByGroupId = mapOf(
                FakeGroupFixtures.owks.groupId to CoreFailure(
                    CoreFailureReason.GROUP_LIST_FAILED,
                    "archive failed",
                ),
            ),
        )
        val targets = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups().selectAllGroupsMode()
        val sent = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        )
            .updateSubject("Event tonight")
            .updateBody("Doors at 9")
            .updateTargetSet(targets.targetSet)
            .sendNotices()

        assertEquals(HostessTextKey.SomeNoticesUnconfirmed, sent.state.sendFooterState.statusTextKey)
        assertEquals("Owks: archive failed", sent.state.sendFooterState.detailText)
    }

    @Test
    fun sendNoticesListsTransportFailuresBeforeArchiveProofGaps() {
        val runtime = FakeHostessUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeRecorder = FakeNoticeRecorder(listOf(GroupSendState.SENT, GroupSendState.FAILED)),
            noticeArchiveEntriesByGroupId = emptyMap(),
        )
        val targets = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups().selectAllGroupsMode()
        val sent = NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        )
            .updateSubject("Event tonight")
            .updateBody("Doors at 9")
            .updateTargetSet(targets.targetSet)
            .sendNotices()

        assertEquals(HostessTextKey.SomeNoticesFailed, sent.state.sendFooterState.statusTextKey)
        assertEquals(
            "m!nx: failed | Owks: notice archive proof_gap subject_or_attachment_not_found",
            sent.state.sendFooterState.detailText,
        )
    }
}
