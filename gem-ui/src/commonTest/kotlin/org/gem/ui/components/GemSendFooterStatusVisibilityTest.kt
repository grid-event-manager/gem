package org.gem.ui.components

import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemSendFooterStatusVisibilityTest {
    @Test
    fun sendProgressAndSuccessMoveOutOfFooter() {
        assertFalse(sendFooterDisplaysStatus(GemTextKey.SendingNotices))
        assertFalse(sendFooterDisplaysStatus(GemTextKey.NoticesSent))
    }

    @Test
    fun readyAndAppOwnedFailuresRemainInFooter() {
        assertTrue(sendFooterDisplaysStatus(GemTextKey.Ready))
        assertTrue(sendFooterDisplaysStatus(GemTextKey.SomeNoticesFailed))
    }
}
