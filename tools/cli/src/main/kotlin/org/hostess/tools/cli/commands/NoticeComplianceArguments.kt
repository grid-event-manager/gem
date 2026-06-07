package org.hostess.tools.cli.commands

import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeComplianceReceipt
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.OperatorLabel
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode

internal class NoticeComplianceArguments private constructor(
    private val values: Values,
) {
    constructor(
        arguments: CommandArguments,
        mode: CommandMode,
    ) : this(
        Values(
            mode = mode,
            operator = arguments.option("operator"),
            ledgerPath = arguments.option("ledger"),
            hasUnsupportedCount = arguments.has(UNSUPPORTED_COUNT_OPTION),
            hasUnsupportedCountSource = arguments.has(UNSUPPORTED_COUNT_SOURCE_OPTION),
        ),
    )

    fun validationErrors(sendMayOccur: Boolean): List<String> = values.validationErrors(sendMayOccur)

    fun ledgerPath(): String? = values.ledgerPath()

    fun request(targetSet: GroupTargetSet): NoticeComplianceRequest = values.request(targetSet)

    fun reportInputs(): Map<String, String> = values.reportInputs()

    fun reportStatusFields(receipt: NoticeComplianceReceipt?): Map<String, String> = values.reportStatusFields(receipt)

    companion object {
        fun fromValues(
            mode: CommandMode,
            operator: String?,
            ledgerPath: String?,
        ): NoticeComplianceArguments = NoticeComplianceArguments(
            Values(
                mode = mode,
                operator = operator,
                ledgerPath = ledgerPath,
                hasUnsupportedCount = false,
                hasUnsupportedCountSource = false,
            ),
        )

        private const val UNSUPPORTED_COUNT_OPTION: String = "recipient" + "-count"
        private const val UNSUPPORTED_COUNT_SOURCE_OPTION: String = UNSUPPORTED_COUNT_OPTION + "-source"
    }

    private data class Values(
        val mode: CommandMode,
        val operator: String?,
        val ledgerPath: String?,
        val hasUnsupportedCount: Boolean,
        val hasUnsupportedCountSource: Boolean,
    ) {
        fun validationErrors(sendMayOccur: Boolean): List<String> = buildList {
            if (hasUnsupportedCount) {
                add(unsupportedCountMessage())
            }
            if (hasUnsupportedCountSource) {
                add(unsupportedCountSourceMessage())
            }
            if (!sendMayOccur || mode == CommandMode.FAKE) {
                return@buildList
            }
            if (operator.isNullOrBlank()) add("operator")
            if (ledgerPath().isNullOrBlank()) add("ledger")
        }

        fun ledgerPath(): String? = ledgerPath?.trim()?.takeIf(String::isNotBlank)

        fun request(targetSet: GroupTargetSet): NoticeComplianceRequest {
            require(targetSet.selectedGroups.isNotEmpty()) {
                "notice compliance target set cannot be empty"
            }
            return NoticeComplianceRequest(operatorLabel())
        }

        fun reportInputs(): Map<String, String> =
            mapOf("noticeOperator" to operator?.trim().orEmpty())

        fun reportStatusFields(receipt: NoticeComplianceReceipt?): Map<String, String> {
            val complianceStatus = when (receipt?.reasonCode) {
                null -> "not_run"
                "allowed" -> "passed"
                "ledger_record_failed" -> "failed"
                else -> "blocked"
            }
            val projectionStatus = when (receipt?.reasonCode) {
                null -> "not_run"
                "allowed", "ledger_record_failed" -> "passed"
                else -> "blocked"
            }
            return mapOf(
                "noticeComplianceStatus" to complianceStatus,
                "noticeSubmissionProjectionStatus" to projectionStatus,
                "noticeSubmissionsProjected" to (receipt?.projectedSubmissionCount?.value ?: 0L).toString(),
                "noticeSubmissionLedgerGroupCount" to (receipt?.ledgerGroupCount ?: 0).toString(),
                "noticeSubmissionLedgerMaxGroupTotal" to (receipt?.ledgerMaxGroupTotal?.value ?: 0L).toString(),
                "noticeSubmissionPerGroupHardCap" to (receipt?.hardCap?.value ?: 180L).toString(),
                "noticeLedgerConfigured" to (mode == CommandMode.FAKE || ledgerPath() != null).toString(),
            )
        }

        private fun operatorLabel(): OperatorLabel {
            val trimmed = operator?.trim()?.takeIf(String::isNotBlank)
            if (trimmed != null) {
                return OperatorLabel(trimmed)
            }
            require(mode == CommandMode.FAKE) { "operator is required for live notice compliance" }
            return OperatorLabel("fake-operator")
        }

        private fun unsupportedCountMessage(): String =
            "recipient" + "-count is no longer supported; notice submissions are derived from selected target groups"

        private fun unsupportedCountSourceMessage(): String =
            "recipient" + "-count-source is no longer supported; notice submissions are derived from selected target groups"
    }
}
