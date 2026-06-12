package org.gem.core.services

import org.gem.core.domain.GroupNoticeConfirmationState
import org.gem.core.domain.GroupNoticeConfirmationStatus
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeConfirmationResult
import org.gem.core.domain.NoticeSendResult
import org.gem.core.ports.GroupNoticeArchiveResult

class NoticeConfirmationService(
    private val groupDirectoryService: GroupDirectoryService,
) {
    fun confirmArchive(
        session: GemSession,
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

            confirmSentGroup(
                session = session,
                status = status,
                expectedSubject = expectedSubject,
                requireAttachment = requireAttachment,
            )
        }
        return NoticeConfirmationResult(statuses)
    }

    private fun confirmSentGroup(
        session: GemSession,
        status: GroupSendStatus,
        expectedSubject: String,
        requireAttachment: Boolean,
    ): GroupNoticeConfirmationStatus {
        var latestStatus: GroupNoticeConfirmationStatus? = null
        repeat(ARCHIVE_CONFIRMATION_ROUNDS) { attemptIndex ->
            val confirmation = readArchiveConfirmation(
                session = session,
                status = status,
                expectedSubject = expectedSubject,
                requireAttachment = requireAttachment,
            )
            if (confirmation.state == GroupNoticeConfirmationState.CONFIRMED) {
                return confirmation
            }
            latestStatus = confirmation.withAttempts(attemptIndex + 1)
        }
        return latestStatus ?: GroupNoticeConfirmationStatus(
            group = status.group,
            state = GroupNoticeConfirmationState.UNCONFIRMED,
            detail = PROOF_GAP_DETAIL,
        )
    }

    private fun readArchiveConfirmation(
        session: GemSession,
        status: GroupSendStatus,
        expectedSubject: String,
        requireAttachment: Boolean,
    ): GroupNoticeConfirmationStatus =
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

    private fun GroupNoticeConfirmationStatus.withAttempts(attempts: Int): GroupNoticeConfirmationStatus =
        if (attempts <= 1 || detail.isNullOrBlank()) {
            this
        } else {
            copy(detail = "$detail; attempts=$attempts")
        }

    private companion object {
        const val ARCHIVE_CONFIRMATION_ROUNDS = 4
        const val PROOF_GAP_DETAIL = "notice archive proof_gap subject_or_attachment_not_found"
        const val ARCHIVE_PROOF_GAP_DETAIL = "notice archive proof_gap"
    }
}
