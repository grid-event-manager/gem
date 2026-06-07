package org.hostess.core.domain

sealed interface NoticeComplianceDecision {
    val receipt: NoticeComplianceReceipt

    data class Allowed(
        val projection: NoticeSubmissionProjection,
        override val receipt: NoticeComplianceReceipt,
    ) : NoticeComplianceDecision

    data class Denied(override val receipt: NoticeComplianceReceipt) : NoticeComplianceDecision
}
