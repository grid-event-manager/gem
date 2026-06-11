package org.hostess.ui.controllers

import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraftInvalidReason
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

    fun hideSendRequirements(): NoticeComposerController =
        copy(
            state.copy(
                sendFooterState = state.sendFooterState.copy(showMissingRequirements = false),
            ),
        )

    fun beginSend(): NoticeComposerController {
        if (!state.sendFooterState.enabled || session == null) {
            return copy(
                state.copy(
                    sendFooterState = state.sendFooterState.copy(showMissingRequirements = true),
                ),
            )
        }
        return copy(
            state.copy(
                sendFooterState = state.sendFooterState.copy(
                    statusTextKey = HostessTextKey.SendingNotices,
                    sending = true,
                    showMissingRequirements = false,
                    detailText = null,
                ),
            ),
        )
    }

    fun sendNotices(): NoticeComposerController {
        if (!state.sendFooterState.enabled || session == null) {
            return copy(
                state.copy(
                    sendFooterState = state.sendFooterState.copy(showMissingRequirements = true),
                ),
            )
        }
        val draft = runtime.noticeDraftService.createDraft(
            subject = state.subject,
            message = state.body,
            targetSet = targetSet,
            attachments = selectedAttachment?.let { listOf(it.request) }.orEmpty(),
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
                attachment = selectedAttachment?.attachmentRef,
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
                ),
            )
        }
    }

    private fun projectFooter(
        validation: NoticeDraftValidation,
    ): SendFooterUiState {
        val ready = session != null &&
            avatarReady &&
            validation == NoticeDraftValidation.Valid
        val missingRequirements = if (ready) {
            emptyList()
        } else {
            missingRequirementKeys(validation)
        }
        return SendFooterUiState(
            visible = true,
            statusTextKey = if (ready) HostessTextKey.Ready else HostessTextKey.BlankStatus,
            missingRequirementKeys = missingRequirements,
            showMissingRequirements = false,
            enabled = ready,
            sending = false,
        )
    }

    private fun missingRequirementKeys(validation: NoticeDraftValidation): List<HostessTextKey> {
        val keys = linkedSetOf<HostessTextKey>()
        if (session == null || !avatarReady) {
            keys += HostessTextKey.PreparingAvatar
        }
        val reasons = (validation as? NoticeDraftValidation.Invalid)?.reasons.orEmpty()
        if (NoticeDraftInvalidReason.BLANK_SUBJECT in reasons) {
            keys += HostessTextKey.MissingSubject
        }
        if (NoticeDraftInvalidReason.BLANK_MESSAGE in reasons) {
            keys += HostessTextKey.MissingBody
        }
        if (NoticeDraftInvalidReason.EMPTY_TARGET_SET in reasons) {
            keys += HostessTextKey.MissingGroups
        }
        return keys.toList()
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
                statusTextKey = if (failedCount == 0) {
                    HostessTextKey.NoticesSent
                } else {
                    HostessTextKey.SomeNoticesFailed
                },
                detailText = failureDetail(result.result.statuses),
                enabled = true,
                sending = false,
                showMissingRequirements = false,
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
                detailText = null,
                enabled = false,
                sending = false,
                showMissingRequirements = true,
            ),
        )

    private fun failureDetail(statuses: List<GroupSendStatus>): String? {
        val failed = statuses.filter { it.state != GroupSendState.SENT }
        if (failed.isEmpty()) {
            return null
        }
        return failed
            .take(MAX_FAILURE_DETAILS)
            .joinToString(separator = " | ") { status ->
                val detail = status.detail?.takeIf(String::isNotBlank) ?: status.state.name.lowercase()
                "${status.group.displayName.value}: $detail"
            }
    }

    private companion object {
        const val MAX_FAILURE_DETAILS: Int = 3
    }
}
