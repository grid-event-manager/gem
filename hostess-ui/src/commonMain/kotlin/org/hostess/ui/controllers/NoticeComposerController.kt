package org.hostess.ui.controllers

import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.NoticeComposerUiState

class NoticeComposerController(
    val runtime: HostessUiRuntime,
    val state: NoticeComposerUiState = NoticeComposerUiState(),
) {
    fun updateSubject(subject: String): NoticeComposerController =
        copy(state.copy(subject = subject))

    fun updateBody(body: String): NoticeComposerController =
        copy(state.copy(body = body, charCount = body.length))

    fun sendNotices(): NoticeComposerController {
        // B-10 owns NoticeDraftService/NoticeDispatchService delegation and send-result projection.
        return copy(state)
    }

    private fun copy(state: NoticeComposerUiState): NoticeComposerController =
        NoticeComposerController(runtime, state)
}
