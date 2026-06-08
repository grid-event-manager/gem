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
            "avatar-readiness",
            "current-groups",
            "select-targets",
            "inventory-catalogue",
            "select-attachment",
            "resolve-attachment",
            "group-notice",
            "notice-archive",
            "cleanup",
            "logout",
        )
        val statusFieldNames = listOf(
            "credentialStatus",
            "loginStatus",
            "avatarReadinessStatus",
            "simulatorPresenceStatus",
            "regionProtocolStatus",
            "agentAppearanceServiceStatus",
            "cofVersionStatus",
            "serverAppearanceStatus",
            "regionHandshakeStatus",
            "regionHandshakeReplyStatus",
            "agentMovementStatus",
            "agentUpdateStatus",
            "currentGroupsStatus",
            "inventoryCatalogueStatus",
            "inventoryItemCount",
            "logoutStatus",
            "attachmentSelectionStatus",
            "attachmentResolutionStatus",
            "noticeSendStatus",
            "noticeArchiveStatus",
            "noticeArchiveTargetCount",
            "noticeArchiveMatchedTargetCount",
            "operatorReceiptStatus",
            "androidProbeStatus",
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
                .plus("noticeArchiveTargetCount" to "0")
                .plus("noticeArchiveMatchedTargetCount" to "0")

        fun after(step: String): String {
            val index = orderedSteps.indexOf(step)
            return orderedSteps.getOrElse(index + 1) { orderedSteps.last() }
        }
    }
}
