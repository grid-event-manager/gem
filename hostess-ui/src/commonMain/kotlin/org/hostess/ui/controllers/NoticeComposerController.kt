package org.hostess.ui.controllers

import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.ports.GroupListResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.state.SelectedAttachmentUiState
import org.hostess.ui.state.NoticeComposerUiState
import org.hostess.ui.state.SendFooterUiState
import org.hostess.ui.text.HostessTextKey

class NoticeComposerController(
    val runtime: HostessUiRuntime,
    val session: HostessSession? = null,
    val avatarReady: Boolean = false,
    val state: NoticeComposerUiState = NoticeComposerUiState(),
    val targetSet: GroupTargetSet = GroupTargetSet.from(emptyList()),
    private val selectedAttachment: SelectedAttachmentUiState? = null,
) {
    fun updateSubject(subject: String): NoticeComposerController =
        copy(projectDraft(subject = subject, body = state.body))

    fun updateBody(body: String): NoticeComposerController =
        copy(projectDraft(subject = state.subject, body = body))

    fun refreshGroups(): GroupTargetController {
        val activeSession = session ?: return GroupTargetController(runtime)
        return when (val result = runtime.groupDirectoryService.currentGroups(activeSession)) {
            is GroupListResult.Success -> GroupTargetController.fromGroups(runtime, result.groups)
            is GroupListResult.Failure -> GroupTargetController(
                runtime = runtime,
                state = GroupTargetUiState(loading = false, errorKey = HostessTextKey.GroupsUnavailable),
            )
        }
    }

    fun updateTargetSet(targetSet: GroupTargetSet): NoticeComposerController =
        copy(
            state = projectDraft(
                subject = state.subject,
                body = state.body,
                targetSet = targetSet,
            ),
            targetSet = targetSet,
        )

    fun updateSelectedAttachment(selectedAttachment: SelectedAttachmentUiState?): NoticeComposerController =
        copy(
            state = projectDraft(
                subject = state.subject,
                body = state.body,
                selectedAttachment = selectedAttachment,
            ),
            selectedAttachment = selectedAttachment,
        )

    fun sendNotices(): NoticeComposerController {
        if (!state.sendFooterState.enabled || session == null || selectedAttachment == null) {
            return copy(state)
        }
        val draft = runtime.noticeDraftService.createDraft(
            subject = state.subject,
            message = state.body,
            targetSet = targetSet,
            attachments = listOf(selectedAttachment.request),
        )
        val validation = runtime.noticeDraftService.validateForSend(draft)
        if (validation != NoticeDraftValidation.Valid) {
            return copy(projectDispatchRejected(validation))
        }
        return when (
            val result = runtime.noticeDispatchService.dispatch(
                session = session,
                draft = draft,
                pacingPolicy = PacingPolicy.NONE,
                attachment = selectedAttachment.attachmentRef,
            )
        ) {
            is NoticeDispatchResult.Sent -> copy(projectDispatchSent(result))
            is NoticeDispatchResult.Rejected -> copy(projectDispatchRejected(result.validation))
        }
    }

    private fun copy(
        state: NoticeComposerUiState,
        targetSet: GroupTargetSet = this.targetSet,
        selectedAttachment: SelectedAttachmentUiState? = this.selectedAttachment,
    ): NoticeComposerController =
        NoticeComposerController(runtime, session, avatarReady, state, targetSet, selectedAttachment)

    private fun projectDraft(
        subject: String,
        body: String,
        targetSet: GroupTargetSet = this.targetSet,
        selectedAttachment: SelectedAttachmentUiState? = this.selectedAttachment,
    ): NoticeComposerUiState {
        val draft = runtime.noticeDraftService.createDraft(
            subject = subject,
            message = body,
            targetSet = targetSet,
            attachments = selectedAttachment?.let { listOf(it.request) }.orEmpty(),
        )
        val attachmentReady = selectedAttachment?.attachmentRef != null
        return when (val validation = runtime.noticeDraftService.validateForSend(draft)) {
            NoticeDraftValidation.Valid -> state.copy(
                subject = subject,
                body = body,
                charCount = body.length,
                draftValid = true,
                draftInvalidReasons = emptySet(),
                selectedTargetSummary = HostessTextKey.SelectedCount(targetSet.selectedCount),
                selectedAttachmentSummary = if (attachmentReady) {
                    HostessTextKey.SelectedCount(1)
                } else {
                    HostessTextKey.None
                },
                sendFooterState = projectFooter(
                    validation = validation,
                    attachmentReady = attachmentReady,
                ),
            )
            is NoticeDraftValidation.Invalid -> state.copy(
                subject = subject,
                body = body,
                charCount = body.length,
                draftValid = false,
                draftInvalidReasons = validation.reasons,
                selectedTargetSummary = HostessTextKey.SelectedCount(targetSet.selectedCount),
                selectedAttachmentSummary = if (attachmentReady) {
                    HostessTextKey.SelectedCount(1)
                } else {
                    HostessTextKey.None
                },
                sendFooterState = projectFooter(
                    validation = validation,
                    attachmentReady = attachmentReady,
                ),
            )
        }
    }

    private fun projectFooter(
        validation: NoticeDraftValidation,
        attachmentReady: Boolean,
    ): SendFooterUiState {
        val ready = session != null &&
            avatarReady &&
            attachmentReady &&
            validation == NoticeDraftValidation.Valid
        return SendFooterUiState(
            visible = true,
            statusTextKey = if (ready) HostessTextKey.Ready else HostessTextKey.BlankStatus,
            enabled = ready,
            sending = false,
        )
    }

    private fun projectDispatchSent(result: NoticeDispatchResult.Sent): NoticeComposerUiState {
        val sentCount = result.result.statuses.count { it.state == GroupSendState.SENT }
        val failedCount = result.result.statuses.count { it.state != GroupSendState.SENT }
        return state.copy(
            sendAttempted = true,
            sentGroupCount = sentCount,
            failedGroupCount = failedCount,
            dispatchRejected = false,
            sendFooterState = state.sendFooterState.copy(
                statusTextKey = HostessTextKey.Ready,
                enabled = true,
                sending = false,
            ),
        )
    }

    private fun projectDispatchRejected(validation: NoticeDraftValidation): NoticeComposerUiState =
        state.copy(
            draftValid = false,
            draftInvalidReasons = (validation as? NoticeDraftValidation.Invalid)?.reasons.orEmpty(),
            sendAttempted = true,
            sentGroupCount = 0,
            failedGroupCount = 0,
            dispatchRejected = true,
            sendFooterState = SendFooterUiState(
                visible = true,
                statusTextKey = HostessTextKey.BlankStatus,
                enabled = false,
                sending = false,
            ),
        )
}
