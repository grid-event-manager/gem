package org.gem.ui

import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemOperationModalRoutingTest {
    @Test
    fun blockingOperationTakesModalPriority() {
        val modal = operationModalFor(
            blockingOperationMessageKey = GemTextKey.LoggingOut,
            loginOperationMessageKey = GemTextKey.LoadingAvatar,
            sendInFlight = true,
            confirmationMessageKey = GemTextKey.NoticesSent,
        )

        assertEquals(GemTextKey.LoggingOut, modal?.messageKey)
        assertTrue(modal?.showSpinner == true)
    }

    @Test
    fun loginProgressTakesPriorityOverSendAndConfirmation() {
        val modal = operationModalFor(
            blockingOperationMessageKey = null,
            loginOperationMessageKey = GemTextKey.LoadingAvatar,
            sendInFlight = true,
            confirmationMessageKey = GemTextKey.AttachmentAdded,
        )

        assertEquals(GemTextKey.LoadingAvatar, modal?.messageKey)
        assertTrue(modal?.showSpinner == true)
    }

    @Test
    fun sendProgressTakesPriorityOverConfirmation() {
        val modal = operationModalFor(
            blockingOperationMessageKey = null,
            loginOperationMessageKey = null,
            sendInFlight = true,
            confirmationMessageKey = GemTextKey.AttachmentAdded,
        )

        assertEquals(GemTextKey.SendingNotices, modal?.messageKey)
        assertTrue(modal?.showSpinner == true)
    }

    @Test
    fun confirmationUsesNonSpinnerModal() {
        val modal = operationModalFor(
            blockingOperationMessageKey = null,
            loginOperationMessageKey = null,
            sendInFlight = false,
            confirmationMessageKey = GemTextKey.AttachmentRemoved,
        )

        assertEquals(GemTextKey.AttachmentRemoved, modal?.messageKey)
        assertFalse(modal?.showSpinner == true)
    }

    @Test
    fun noOperationHidesModal() {
        assertNull(
            operationModalFor(
                blockingOperationMessageKey = null,
                loginOperationMessageKey = null,
                sendInFlight = false,
                confirmationMessageKey = null,
            ),
        )
    }
}
