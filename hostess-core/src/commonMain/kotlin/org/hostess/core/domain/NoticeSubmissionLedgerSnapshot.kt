package org.hostess.core.domain

data class NoticeSubmissionLedgerSnapshot(
    val proofAccountLabel: AccountLabel,
    val groupId: GroupId,
    val groupDisplayName: GroupDisplayName,
    val noticeLedgerDay: NoticeLedgerDay,
    val reservedSubmissionCount: NoticeSubmissionCount,
    val recordedSentSubmissionCount: NoticeSubmissionCount,
    val lastOperatorLabel: OperatorLabel,
)
