package org.gem.ui.controllers

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupSendState
import org.gem.ui.testing.FakeGroupFixtures
import org.gem.core.domain.NoticeDraftInvalidReason
import org.gem.ui.testing.FakeInventoryFixtures
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeNoticeRecorder
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeComposerControllerTest {
    @Test
    fun subjectAndUnicodeBodyRoundTripThroughCoreDraftProjection() {
        val body = "Tonight at 9 \uD83C\uDFA7"
        val controller = NoticeComposerController(FakeGemUiRuntime.ready())
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
        val controller = NoticeComposerController(FakeGemUiRuntime.ready())
            .updateSubject("Event tonight")
            .updateBody("")

        assertTrue(NoticeDraftInvalidReason.BLANK_MESSAGE in controller.state.draftInvalidReasons)
    }

    @Test
    fun sendNoticesProjectsSendingAndSentFeedback() {
        val recorder = org.gem.ui.testing.FakeNoticeRecorder()
        val runtime = FakeGemUiRuntime.ready(
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
        assertEquals(GemTextKey.SendingNotices, sending.state.sendFooterState.statusTextKey)
        assertEquals(GemTextKey.NoticesSent, sent.state.sendFooterState.statusTextKey)
        assertFalse(sent.state.sendFooterState.sending)
        assertEquals(2, recorder.sendCallCount)
    }

    @Test
    fun sendNoticesProjectsUnconfirmedFeedbackWhenArchiveProofMisses() {
        val runtime = FakeGemUiRuntime.ready(
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

        assertEquals(GemTextKey.SomeNoticesUnconfirmed, sent.state.sendFooterState.statusTextKey)
        assertEquals(
            "Owks: notice archive proof_gap subject_or_attachment_not_found; attempts=4 | " +
                "m!nx: notice archive proof_gap subject_or_attachment_not_found; attempts=4",
            sent.state.sendFooterState.detailText,
        )
    }

    @Test
    fun sendNoticesProjectsArchiveFailureWithoutHidingBehindSuccess() {
        val runtime = FakeGemUiRuntime.ready(
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

        assertEquals(GemTextKey.SomeNoticesUnconfirmed, sent.state.sendFooterState.statusTextKey)
        assertEquals("Owks: archive failed; attempts=4", sent.state.sendFooterState.detailText)
    }

    @Test
    fun sendNoticesListsTransportFailuresBeforeArchiveProofGaps() {
        val runtime = FakeGemUiRuntime.ready(
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

        assertEquals(GemTextKey.SomeNoticesFailed, sent.state.sendFooterState.statusTextKey)
        assertEquals(
            "m!nx: failed | Owks: notice archive proof_gap subject_or_attachment_not_found; attempts=4",
            sent.state.sendFooterState.detailText,
        )
    }
}
