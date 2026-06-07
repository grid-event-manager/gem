package org.hostess.core.domain

data class NoticeComplianceReceipt(
    val operatorLabel: OperatorLabel,
    val proofAccountLabel: AccountLabel,
    val noticeLedgerDay: NoticeLedgerDay,
    val projectedSubmissionCount: NoticeSubmissionCount,
    val ledgerGroupCount: Int,
    val ledgerMaxGroupTotal: NoticeSubmissionCount,
    val hardCap: NoticeSubmissionCount,
    val reasonCode: String,
    val redactedSourceSummary: String,
)
