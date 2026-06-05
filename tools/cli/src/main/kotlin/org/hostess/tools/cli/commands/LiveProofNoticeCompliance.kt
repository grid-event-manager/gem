package org.hostess.tools.cli.commands

import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeComplianceReceipt
import org.hostess.core.domain.NoticeComplianceRequest

internal class LiveProofNoticeCompliance(
    private val arguments: NoticeComplianceArguments,
) {
    constructor(inputs: LiveProofInputs) : this(inputs.noticeComplianceArguments())

    fun missingRequiredFields(sendMayOccur: Boolean): List<String> =
        arguments.missingRequiredFields(sendMayOccur)

    fun request(targetSet: GroupTargetSet): NoticeComplianceRequest =
        arguments.request(targetSet)

    fun reportInputs(): Map<String, String> =
        arguments.reportInputs()

    fun reportStatusFields(receipt: NoticeComplianceReceipt?): Map<String, String> =
        arguments.reportStatusFields(receipt)
}
