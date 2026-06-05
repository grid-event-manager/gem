package org.hostess.tools.cli.commands

import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeComplianceReceipt
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeRecipientCount
import org.hostess.core.domain.NoticeRecipientEstimate
import org.hostess.core.domain.NoticeRecipientEstimateSource
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
            recipientCountValues = arguments.optionValues("recipient-count"),
            recipientCountSource = arguments.option("recipient-count-source"),
            ledgerPath = arguments.option("ledger"),
        ),
    )

    fun missingRequiredFields(sendMayOccur: Boolean): List<String> = values.missingRequiredFields(sendMayOccur)

    fun ledgerPath(): String? = values.ledgerPath()

    fun request(targetSet: GroupTargetSet): NoticeComplianceRequest = values.request(targetSet)

    fun reportInputs(): Map<String, String> = values.reportInputs()

    fun reportStatusFields(receipt: NoticeComplianceReceipt?): Map<String, String> = values.reportStatusFields(receipt)

    companion object {
        fun fromValues(
            mode: CommandMode,
            operator: String?,
            recipientCountValues: List<String>,
            recipientCountSource: String?,
            ledgerPath: String?,
        ): NoticeComplianceArguments = NoticeComplianceArguments(
            Values(
                mode = mode,
                operator = operator,
                recipientCountValues = recipientCountValues,
                recipientCountSource = recipientCountSource,
                ledgerPath = ledgerPath,
            ),
        )
    }

    private data class Values(
        val mode: CommandMode,
        val operator: String?,
        val recipientCountValues: List<String>,
        val recipientCountSource: String?,
        val ledgerPath: String?,
    ) {
        fun missingRequiredFields(sendMayOccur: Boolean): List<String> = buildList {
            if (!sendMayOccur || mode == CommandMode.FAKE) {
                return@buildList
            }
            if (operator.isNullOrBlank()) add("operator")
            if (source() == null) add("recipient-count-source")
            if (recipientCountValues.isEmpty() || parsedCounts() == null) add("recipient-count")
            if (ledgerPath().isNullOrBlank()) add("ledger")
        }

        fun ledgerPath(): String? = ledgerPath?.trim()?.takeIf(String::isNotBlank)

        fun request(targetSet: GroupTargetSet): NoticeComplianceRequest {
            val selectedGroups = targetSet.selectedGroups
            require(selectedGroups.isNotEmpty()) { "notice compliance target set cannot be empty" }

            val estimatesByName = estimates(targetSet).groupBy { it.displayName }
            val selectedNames = selectedGroups.map { it.displayName }
            val unknownName = estimatesByName.keys.firstOrNull { it !in selectedNames.toSet() }
            require(unknownName == null) { "recipient-count must name only selected groups" }

            val orderedEstimates = selectedNames.map { displayName ->
                val estimates = estimatesByName[displayName].orEmpty()
                require(estimates.size == 1) { "recipient-count must include exactly one entry per selected group" }
                estimates.single()
            }

            return NoticeComplianceRequest(
                operatorLabel = operatorLabel(),
                recipientEstimates = orderedEstimates,
            )
        }

        fun reportInputs(): Map<String, String> = buildMap {
            put("noticeOperator", operator?.trim().orEmpty())
            put("recipientCountSource", reportSourceValue())
            put("recipientEstimateCount", estimateCount().toString())
        }

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
                "recipientProjectionStatus" to projectionStatus,
                "recipientDeliveryProjected" to (receipt?.projectedDeliveryCount?.value ?: 0L).toString(),
                "recipientDeliveryLedgerTotal" to (receipt?.projectedLedgerTotal?.value ?: 0L).toString(),
                "recipientDeliveryHardCap" to (receipt?.hardCap?.value ?: 4_500L).toString(),
                "noticeLedgerConfigured" to (mode == CommandMode.FAKE || ledgerPath() != null).toString(),
            )
        }

        private fun estimates(targetSet: GroupTargetSet): List<NoticeRecipientEstimate> {
            if (!recipientCountSource.isNullOrBlank() && source() == null) {
                throw IllegalArgumentException(
                    "recipient-count-source must be operator-acknowledged or authoritative",
                )
            }
            if (recipientCountValues.isEmpty() && mode == CommandMode.FAKE) {
                return targetSet.selectedGroups.map { group ->
                    NoticeRecipientEstimate(
                        displayName = group.displayName,
                        recipientCount = NoticeRecipientCount(1),
                        source = NoticeRecipientEstimateSource.AUTHORITATIVE,
                    )
                }
            }

            return parsedCounts() ?: throw IllegalArgumentException("recipient-count must be <display-name=count>")
        }

        private fun parsedCounts(): List<NoticeRecipientEstimate>? {
            val source = sourceOrDefault()
            return recipientCountValues.map { raw ->
                val separator = raw.indexOf('=')
                if (separator <= 0 || separator == raw.lastIndex) {
                    return null
                }
                val displayName = raw.substring(0, separator).trim().takeIf(String::isNotBlank) ?: return null
                val count = raw.substring(separator + 1).trim().toLongOrNull() ?: return null
                try {
                    NoticeRecipientEstimate(
                        displayName = GroupDisplayName(displayName),
                        recipientCount = NoticeRecipientCount(count),
                        source = source,
                    )
                } catch (_: IllegalArgumentException) {
                    return null
                }
            }
        }

        private fun sourceOrDefault(): NoticeRecipientEstimateSource =
            source() ?: NoticeRecipientEstimateSource.AUTHORITATIVE

        private fun source(): NoticeRecipientEstimateSource? = when (recipientCountSource?.trim()?.lowercase()) {
            "authoritative" -> NoticeRecipientEstimateSource.AUTHORITATIVE
            "operator-acknowledged" -> NoticeRecipientEstimateSource.OPERATOR_ACKNOWLEDGED
            null, "" -> null
            else -> null
        }

        private fun operatorLabel(): OperatorLabel {
            val trimmed = operator?.trim()?.takeIf(String::isNotBlank)
            if (trimmed != null) {
                return OperatorLabel(trimmed)
            }
            require(mode == CommandMode.FAKE) { "operator is required for live notice compliance" }
            return OperatorLabel("fake-operator")
        }

        private fun estimateCount(): Int =
            parsedCounts()?.size ?: if (mode == CommandMode.FAKE && recipientCountValues.isEmpty()) 0 else recipientCountValues.size

        private fun reportSourceValue(): String =
            source()?.wireValue()
                ?: if (mode == CommandMode.FAKE && recipientCountValues.isEmpty()) {
                    NoticeRecipientEstimateSource.AUTHORITATIVE.wireValue()
                } else {
                    ""
                }
    }
}

private fun NoticeRecipientEstimateSource.wireValue(): String =
    name.lowercase().replace('_', '-')
