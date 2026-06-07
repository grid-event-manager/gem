package org.hostess.core.ports

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionLedgerSnapshot
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.domain.OperatorLabel

interface NoticeSubmissionLedgerPort {
    fun snapshot(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        groups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>

    fun reserve(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>

    fun recordSendResult(
        proofAccountLabel: AccountLabel,
        operatorLabel: OperatorLabel,
        projection: NoticeSubmissionProjection,
        sentGroups: List<GroupMembership>,
        noticeLedgerDay: NoticeLedgerDay,
    ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>
}
