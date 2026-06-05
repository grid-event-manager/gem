package org.hostess.apps.android

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.NoticeComplianceLedgerPort
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeComplianceClock
import org.hostess.core.services.NoticeComplianceService
import org.hostess.core.services.TargetSelectionService
import org.hostess.protocol.libomv.ProtocolLibomvModule
import org.hostess.protocol.libomv.runtime.HostessViewerIdentityProvider

class AndroidCompatibilityProbe {
    fun run(): AndroidCompatibilityResult {
        val coreCompile = probeCoreCompile()
        val runtimeResult = runCatching { ProtocolLibomvModule.liveRuntime() }
        val loadState = runtimeResult.getOrNull()?.loadState
        val adapterLoad = loadState?.adapterLoad ?: false
        val runtimeLoad = loadState?.runtimeLoad ?: false
        val transportLoad = loadState?.transportLoad ?: false
        val trackCClassLoad = runtimeLoad && transportLoad
        val trackDComplianceLoad = probeTrackDComplianceLoad()
        val trackDsLoginPackageLoad = probeTrackDsLoginPackageLoad()

        return if (
            coreCompile &&
            adapterLoad &&
            runtimeLoad &&
            transportLoad &&
            trackCClassLoad &&
            trackDComplianceLoad &&
            trackDsLoginPackageLoad
        ) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap(
                coreCompile = coreCompile,
                adapterLoad = adapterLoad,
                runtimeLoad = runtimeLoad,
                transportLoad = transportLoad,
                trackCClassLoad = trackCClassLoad,
                trackDComplianceLoad = trackDComplianceLoad,
                trackDsLoginPackageLoad = trackDsLoginPackageLoad,
                reason = blockedReason(
                    coreCompile,
                    adapterLoad,
                    runtimeLoad,
                    transportLoad,
                    trackCClassLoad,
                    trackDComplianceLoad,
                    trackDsLoginPackageLoad,
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

    private fun probeTrackDComplianceLoad(): Boolean =
        listOf(
            HostessViewerIdentityProvider::class.java,
            LoginComplianceService::class.java,
            NoticeComplianceService::class.java,
            NoticeComplianceClock::class.java,
            NoticeComplianceLedgerPort::class.java,
        ).all { it.name.isNotBlank() }

    private fun probeTrackDsLoginPackageLoad(): Boolean =
        TRACK_DS_LOGIN_PACKAGE_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun blockedReason(
        coreCompile: Boolean,
        adapterLoad: Boolean,
        runtimeLoad: Boolean,
        transportLoad: Boolean,
        trackCClassLoad: Boolean,
        trackDComplianceLoad: Boolean,
        trackDsLoginPackageLoad: Boolean,
        failure: Throwable?,
    ): String {
        val failedLanes = listOfNotNull(
            "coreCompile".takeUnless { coreCompile },
            "adapterLoad".takeUnless { adapterLoad },
            "runtimeLoad".takeUnless { runtimeLoad },
            "transportLoad".takeUnless { transportLoad },
            "trackCClassLoad".takeUnless { trackCClassLoad },
            "trackDComplianceLoad".takeUnless { trackDComplianceLoad },
            "trackDsLoginPackageLoad".takeUnless { trackDsLoginPackageLoad },
        )
        val cause = failure?.let { " cause=${it::class.java.simpleName}" }.orEmpty()
        return "Android compatibility probe failed lanes=${failedLanes.joinToString(",")}$cause"
    }

    private fun probeGroups(): List<GroupMembership> = listOf(
        GroupMembership.fromValues("android-probe-group", "Probe Hosts", true, true),
    )

    private companion object {
        val TRACK_DS_LOGIN_PACKAGE_CLASSES = listOf(
            "org.hostess.protocol.libomv.runtime.LoginPackageBuilder",
            "org.hostess.protocol.libomv.runtime.LoginPackageSerializer",
            "org.hostess.protocol.libomv.runtime.SecondLifePasswordHash",
            "org.hostess.protocol.libomv.runtime.HostessMachineIdentityProvider",
            "org.hostess.protocol.libomv.runtime.DefaultHostessMachineIdentityProvider",
        )
    }
}

data class AndroidCompatibilityResult(
    val status: String,
    val coreCompile: Boolean,
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
    val trackCClassLoad: Boolean,
    val trackDComplianceLoad: Boolean,
    val trackDsLoginPackageLoad: Boolean,
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
            trackDComplianceLoad = true,
            trackDsLoginPackageLoad = true,
            forbiddenApiScan = "external_guard_required",
            blockedReason = null,
        )

        fun androidGap(
            coreCompile: Boolean,
            adapterLoad: Boolean,
            runtimeLoad: Boolean,
            transportLoad: Boolean,
            trackCClassLoad: Boolean,
            trackDComplianceLoad: Boolean,
            trackDsLoginPackageLoad: Boolean,
            reason: String,
        ): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = coreCompile,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
            trackCClassLoad = trackCClassLoad,
            trackDComplianceLoad = trackDComplianceLoad,
            trackDsLoginPackageLoad = trackDsLoginPackageLoad,
            forbiddenApiScan = "external_guard_required",
            blockedReason = reason,
        )
    }
}
