package org.hostess.core.domain

sealed interface NoticeComplianceLedgerResult<out T> {
    data class Success<T>(val value: T) : NoticeComplianceLedgerResult<T>
    data class Failure(val reasonCode: String) : NoticeComplianceLedgerResult<Nothing>
}
