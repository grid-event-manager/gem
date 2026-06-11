package org.hostess.core.services

import org.hostess.core.domain.GroupNoticeConfirmationState
import org.hostess.core.domain.GroupNoticeConfirmationStatus
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeConfirmationResult
import org.hostess.core.domain.NoticeSendResult
import org.hostess.core.ports.GroupNoticeArchiveResult

class NoticeConfirmationService(
    private val groupDirectoryService: GroupDirectoryService,
) {
    fun confirmArchive(
        session: HostessSession,
        sendResult: NoticeSendResult,
    ): NoticeConfirmationResult {
        val expectedSubject = sendResult.plan.draft.subject
        val requireAttachment = sendResult.plan.draft.attachments.isNotEmpty()
        val statuses = sendResult.statuses.map { status ->
            if (status.state != GroupSendState.SENT) {
                return@map GroupNoticeConfirmationStatus(
                    group = status.group,
                    state = GroupNoticeConfirmationState.SKIPPED,
                    detail = transportDetail(status.detail, status.state),
                )
            }

            when (val archive = groupDirectoryService.noticeArchive(session, status.group)) {
                is GroupNoticeArchiveResult.Success -> {
                    val matched = archive.entries.any { entry ->
                        entry.subject == expectedSubject && (!requireAttachment || entry.hasAttachment)
                    }
                    if (matched) {
                        GroupNoticeConfirmationStatus(status.group, GroupNoticeConfirmationState.CONFIRMED)
                    } else {
                        GroupNoticeConfirmationStatus(
                            group = status.group,
                            state = GroupNoticeConfirmationState.UNCONFIRMED,
                            detail = PROOF_GAP_DETAIL,
                        )
                    }
                }
                is GroupNoticeArchiveResult.Failure -> GroupNoticeConfirmationStatus(
                    group = status.group,
                    state = GroupNoticeConfirmationState.FAILED,
                    detail = archiveFailureDetail(archive),
                )
            }
        }
        return NoticeConfirmationResult(statuses)
    }

    private fun transportDetail(
        detail: String?,
        state: GroupSendState,
    ): String = detail
        ?.takeIf(String::isNotBlank)
        ?.let(SafeDiagnosticRedaction::excerpt)
        ?: state.name.lowercase()

    private fun archiveFailureDetail(
        failure: GroupNoticeArchiveResult.Failure,
    ): String = failure.failure.redactedMessage
        ?.takeIf(String::isNotBlank)
        ?.let(SafeDiagnosticRedaction::excerpt)
        ?: ARCHIVE_PROOF_GAP_DETAIL

    private companion object {
        const val PROOF_GAP_DETAIL = "notice archive proof_gap subject_or_attachment_not_found"
        const val ARCHIVE_PROOF_GAP_DETAIL = "notice archive proof_gap"
    }
}
