package org.gem.ui.controllers

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupId
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
    fun sendNoticesKeepsArchiveProofMissesOutOfVisibleFeedback() {
        val archiveReads = mutableListOf<GroupId>()
        val runtime = FakeGemUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeArchiveEntriesByGroupId = emptyMap(),
            noticeArchiveReadLog = archiveReads,
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

        assertEquals(GemTextKey.NoticesSent, sent.state.sendFooterState.statusTextKey)
        assertEquals(null, sent.state.sendFooterState.detailText)
        assertTrue(archiveReads.isEmpty())
    }

    @Test
    fun sendNoticesKeepsArchiveFailuresOutOfVisibleFeedback() {
        val archiveReads = mutableListOf<GroupId>()
        val runtime = FakeGemUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeArchiveFailuresByGroupId = mapOf(
                FakeGroupFixtures.owks.groupId to CoreFailure(
                    CoreFailureReason.GROUP_LIST_FAILED,
                    "archive failed",
                ),
            ),
            noticeArchiveReadLog = archiveReads,
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

        assertEquals(GemTextKey.NoticesSent, sent.state.sendFooterState.statusTextKey)
        assertEquals(null, sent.state.sendFooterState.detailText)
        assertTrue(archiveReads.isEmpty())
    }

    @Test
    fun sendNoticesListsTransportFailuresBeforeArchiveProofGaps() {
        val archiveReads = mutableListOf<GroupId>()
        val runtime = FakeGemUiRuntime.ready(
            groups = FakeGroupFixtures.sendableGroups(),
            noticeRecorder = FakeNoticeRecorder(listOf(GroupSendState.SENT, GroupSendState.FAILED)),
            noticeArchiveEntriesByGroupId = emptyMap(),
            noticeArchiveReadLog = archiveReads,
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
            "m!nx: failed",
            sent.state.sendFooterState.detailText,
        )
        assertTrue(archiveReads.isEmpty())
    }
}
