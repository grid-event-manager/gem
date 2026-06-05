package org.hostess.apps.android

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.services.TargetSelectionService
import org.hostess.protocol.libomv.ProtocolLibomvModule

class AndroidCompatibilityProbe {
    fun run(): AndroidCompatibilityResult {
        val coreCompile = probeCoreCompile()
        val runtimeResult = runCatching { ProtocolLibomvModule.liveRuntime() }
        val loadState = runtimeResult.getOrNull()?.loadState
        val adapterLoad = loadState?.adapterLoad ?: false
        val runtimeLoad = loadState?.runtimeLoad ?: false
        val transportLoad = loadState?.transportLoad ?: false
        val trackCClassLoad = runtimeLoad && transportLoad

        return if (coreCompile && adapterLoad && runtimeLoad && transportLoad && trackCClassLoad) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap(
                coreCompile = coreCompile,
                adapterLoad = adapterLoad,
                runtimeLoad = runtimeLoad,
                transportLoad = transportLoad,
                trackCClassLoad = trackCClassLoad,
                reason = blockedReason(
                    coreCompile,
                    adapterLoad,
                    runtimeLoad,
                    transportLoad,
                    trackCClassLoad,
                    runtimeResult.exceptionOrNull(),
                ),
            )
        }
    }

    private fun probeCoreCompile(): Boolean {
        val targetSelectionService = TargetSelectionService()
        val targetSet = targetSelectionService.emptyTargetSet(probeGroups())
        val selected = targetSelectionService.addTargetByDisplayName(targetSet, "Probe Hosts")
        val selectedTargetSet = when (selected) {
            is TargetSelectionResult.Changed -> selected.targetSet
            else -> return false
        }

        val draft = NoticeDraft(
            subject = "Android compatibility probe",
            message = "No live send.",
            targetSet = selectedTargetSet,
        )
        return draft.validateForSend() == NoticeDraftValidation.Valid
    }

    private fun blockedReason(
        coreCompile: Boolean,
        adapterLoad: Boolean,
        runtimeLoad: Boolean,
        transportLoad: Boolean,
        trackCClassLoad: Boolean,
        failure: Throwable?,
    ): String {
        val failedLanes = listOfNotNull(
            "coreCompile".takeUnless { coreCompile },
            "adapterLoad".takeUnless { adapterLoad },
            "runtimeLoad".takeUnless { runtimeLoad },
            "transportLoad".takeUnless { transportLoad },
            "trackCClassLoad".takeUnless { trackCClassLoad },
        )
        val cause = failure?.let { " cause=${it::class.java.simpleName}" }.orEmpty()
        return "Android compatibility probe failed lanes=${failedLanes.joinToString(",")}$cause"
    }

    private fun probeGroups(): List<GroupMembership> = listOf(
        GroupMembership.fromValues("android-probe-group", "Probe Hosts", true, true),
    )
}

data class AndroidCompatibilityResult(
    val status: String,
    val coreCompile: Boolean,
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
    val trackCClassLoad: Boolean,
    val forbiddenApiScan: String,
    val blockedReason: String?,
) {
    companion object {
        fun passed(): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "passed",
            coreCompile = true,
            adapterLoad = true,
            runtimeLoad = true,
            transportLoad = true,
            trackCClassLoad = true,
            forbiddenApiScan = "external_guard_required",
            blockedReason = null,
        )

        fun androidGap(
            coreCompile: Boolean,
            adapterLoad: Boolean,
            runtimeLoad: Boolean,
            transportLoad: Boolean,
            trackCClassLoad: Boolean,
            reason: String,
        ): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = coreCompile,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
            trackCClassLoad = trackCClassLoad,
            forbiddenApiScan = "external_guard_required",
            blockedReason = reason,
        )
    }
}
