package org.hostess.tools.cli.commands

internal data class LiveProofStep(
    val name: String,
    val state: String,
    val detail: String? = null,
) {
    fun toReportMap(): Map<String, String> = buildMap {
        put("step", name)
        put("state", state)
        detail?.let { put("detail", it) }
    }

    companion object {
        private val orderedSteps = listOf(
            "validate-inputs",
            "login",
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "cleanup",
            "logout",
        )
        val statusFieldNames = listOf(
            "credentialStatus",
            "loginStatus",
            "currentGroupsStatus",
            "inventoryCatalogueStatus",
            "inventoryItemCount",
            "logoutStatus",
            "attachmentSelectionStatus",
            "attachmentResolutionStatus",
            "noticeSendStatus",
            "androidProbeStatus",
        )
        private val noticeComplianceDefaultFields = mapOf(
            "noticeComplianceStatus" to "not_run",
            "noticeSubmissionProjectionStatus" to "not_run",
            "noticeSubmissionsProjected" to "0",
            "noticeSubmissionLedgerGroupCount" to "0",
            "noticeSubmissionLedgerMaxGroupTotal" to "0",
            "noticeSubmissionPerGroupHardCap" to "180",
            "noticeLedgerConfigured" to "false",
        )

        fun passed(name: String, detail: String? = null): LiveProofStep = LiveProofStep(name, "passed", detail)

        fun failed(name: String, detail: String): LiveProofStep = LiveProofStep(name, "failed", detail)

        fun blockedPlan(blockedStep: String, detail: String): List<LiveProofStep> =
            listOf(LiveProofStep(blockedStep, "blocked", detail)) + notRunPlan(detail, after(blockedStep))

        fun notRunPlan(detail: String, startAt: String = orderedSteps.first()): List<LiveProofStep> =
            orderedSteps.dropWhile { it != startAt }.map { LiveProofStep(it, "not_run", detail) }

        fun statusFields(default: String = "not_run"): Map<String, String> =
            statusFieldNames.associateWith { default }
                .plus("inventoryItemCount" to "0")
                .plus(noticeComplianceDefaultFields.copyWithStatus(default))

        fun after(step: String): String {
            val index = orderedSteps.indexOf(step)
            return orderedSteps.getOrElse(index + 1) { orderedSteps.last() }
        }
    }
}

private fun Map<String, String>.copyWithStatus(default: String): Map<String, String> =
    this + mapOf(
        "noticeComplianceStatus" to default,
        "noticeSubmissionProjectionStatus" to default,
    )
