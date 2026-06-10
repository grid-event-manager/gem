package org.hostess.ui.controllers

import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.NoticeComposerUiState

class NoticeComposerController(
    val runtime: HostessUiRuntime,
    val state: NoticeComposerUiState = NoticeComposerUiState(),
) {
    fun updateSubject(subject: String): NoticeComposerController =
        copy(projectDraft(subject = subject, body = state.body))

    fun updateBody(body: String): NoticeComposerController =
        copy(projectDraft(subject = state.subject, body = body))

    fun sendNotices(): NoticeComposerController {
        // B-10 owns NoticeDraftService/NoticeDispatchService delegation and send-result projection.
        return copy(state)
    }

    private fun copy(state: NoticeComposerUiState): NoticeComposerController =
        NoticeComposerController(runtime, state)

    private fun projectDraft(
        subject: String,
        body: String,
    ): NoticeComposerUiState {
        val draft = runtime.noticeDraftService.createDraft(
            subject = subject,
            message = body,
            targetSet = GroupTargetSet.from(emptyList()),
        )
        return when (val validation = runtime.noticeDraftService.validateForSend(draft)) {
            NoticeDraftValidation.Valid -> state.copy(
                subject = subject,
                body = body,
                charCount = body.length,
                draftValid = true,
                draftInvalidReasons = emptySet(),
            )
            is NoticeDraftValidation.Invalid -> state.copy(
                subject = subject,
                body = body,
                charCount = body.length,
                draftValid = false,
                draftInvalidReasons = validation.reasons,
            )
        }
    }
}
