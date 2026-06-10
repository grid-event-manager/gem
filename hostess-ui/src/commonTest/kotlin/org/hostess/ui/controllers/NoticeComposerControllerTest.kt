package org.hostess.ui.controllers

import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.ui.testing.FakeHostessUiRuntime
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
}
