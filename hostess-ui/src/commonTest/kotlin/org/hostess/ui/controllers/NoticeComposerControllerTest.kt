package org.hostess.ui.controllers

import org.hostess.ui.testing.FakeGroupFixtures
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.testing.FakeHostessUiRuntime
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
}
