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
            "plain-notice",
            "landmark-attachment",
            "landmark-notice",
            "texture-attachment",
            "texture-notice",
            "bulk-notice",
            "cleanup",
            "logout",
        )
        val statusFieldNames = listOf(
            "loginStatus",
            "currentGroupsStatus",
            "plainNoticeStatus",
            "landmarkAttachmentStatus",
            "textureAttachmentStatus",
            "bulkNoticeStatus",
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

        fun after(step: String): String {
            val index = orderedSteps.indexOf(step)
            return orderedSteps.getOrElse(index + 1) { orderedSteps.last() }
        }
    }
}
