package org.gem.ui.controllers

import org.gem.core.domain.GroupNoticeConfirmationState
import org.gem.core.domain.GroupNoticeConfirmationStatus
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDispatchResult
import org.gem.core.domain.NoticeDraftInvalidReason
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.NoticeConfirmationResult
import org.gem.core.domain.PacingPolicy
import org.gem.core.ports.GroupListResult
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.GroupTargetUiState
import org.gem.ui.state.SelectedAttachmentUiState
import org.gem.ui.state.NoticeComposerUiState
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.text.GemTextKey

class NoticeComposerController(
    val runtime: GemUiRuntime,
    val session: GemSession? = null,
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
                state = GroupTargetUiState(loading = false, errorKey = GemTextKey.GroupsUnavailable),
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
                    statusTextKey = GemTextKey.SendingNotices,
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
            is NoticeDispatchResult.Sent -> {
                val confirmation = runtime.noticeConfirmationService.confirmArchive(session, result.result)
                copy(projectDispatchSent(result, confirmation))
            }
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
                selectedTargetSummary = GemTextKey.SelectedCount(targetSet.selectedCount),
                selectedAttachmentSummary = if (attachmentReady) {
                    GemTextKey.SelectedCount(1)
                } else {
                    GemTextKey.None
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
                selectedTargetSummary = GemTextKey.SelectedCount(targetSet.selectedCount),
                selectedAttachmentSummary = if (attachmentReady) {
                    GemTextKey.SelectedCount(1)
                } else {
                    GemTextKey.None
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
            statusTextKey = if (ready) GemTextKey.Ready else GemTextKey.BlankStatus,
            missingRequirementKeys = missingRequirements,
            showMissingRequirements = false,
            enabled = ready,
            sending = false,
        )
    }

    private fun missingRequirementKeys(validation: NoticeDraftValidation): List<GemTextKey> {
        val keys = linkedSetOf<GemTextKey>()
        if (session == null || !avatarReady) {
            keys += GemTextKey.PreparingAvatar
        }
        val reasons = (validation as? NoticeDraftValidation.Invalid)?.reasons.orEmpty()
        if (NoticeDraftInvalidReason.BLANK_SUBJECT in reasons) {
            keys += GemTextKey.MissingSubject
        }
        if (NoticeDraftInvalidReason.BLANK_MESSAGE in reasons) {
            keys += GemTextKey.MissingBody
        }
        if (NoticeDraftInvalidReason.EMPTY_TARGET_SET in reasons) {
            keys += GemTextKey.MissingGroups
        }
        return keys.toList()
    }

    private fun projectDispatchSent(
        result: NoticeDispatchResult.Sent,
        confirmation: NoticeConfirmationResult,
    ): NoticeComposerUiState {
        val sentCount = result.result.statuses.count { it.state == GroupSendState.SENT }
        val failedCount = result.result.statuses.count { it.state != GroupSendState.SENT }
        val hasTransportFailure = failedCount > 0
        return state.copy(
            sendAttempted = true,
            sentGroupCount = sentCount,
            failedGroupCount = failedCount,
            dispatchRejected = false,
            sendFooterState = state.sendFooterState.copy(
                statusTextKey = sendStatusTextKey(hasTransportFailure, confirmation),
                detailText = failureDetail(result.result.statuses, confirmation.statuses),
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
                statusTextKey = GemTextKey.BlankStatus,
                detailText = null,
                enabled = false,
                sending = false,
                showMissingRequirements = true,
            ),
        )

    private fun sendStatusTextKey(
        hasTransportFailure: Boolean,
        confirmation: NoticeConfirmationResult,
    ): GemTextKey =
        when {
            hasTransportFailure -> GemTextKey.SomeNoticesFailed
            confirmation.allConfirmed -> GemTextKey.NoticesSent
            else -> GemTextKey.SomeNoticesUnconfirmed
        }

    private fun failureDetail(
        statuses: List<GroupSendStatus>,
        confirmationStatuses: List<GroupNoticeConfirmationStatus>,
    ): String? {
        val failed = statuses.filter { it.state != GroupSendState.SENT }
        val confirmationGaps = confirmationStatuses.filter {
            it.state == GroupNoticeConfirmationState.UNCONFIRMED ||
                it.state == GroupNoticeConfirmationState.FAILED
        }
        if (failed.isEmpty() && confirmationGaps.isEmpty()) {
            return null
        }
        val details = mutableListOf<String>()
        failed.take(MAX_FAILURE_DETAILS).mapTo(details) { status ->
            val detail = status.detail?.takeIf(String::isNotBlank) ?: status.state.name.lowercase()
            "${status.group.displayName.value}: $detail"
        }
        if (details.size < MAX_FAILURE_DETAILS) {
            confirmationGaps.take(MAX_FAILURE_DETAILS - details.size).mapTo(details) { status ->
                val detail = status.detail?.takeIf(String::isNotBlank) ?: status.state.name.lowercase()
                "${status.group.displayName.value}: $detail"
            }
        }
        return details.joinToString(separator = " | ")
    }

    private companion object {
        const val MAX_FAILURE_DETAILS: Int = 3
    }
}
