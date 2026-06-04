package org.hostess.apps.android

import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.services.TargetSelectionService
import org.hostess.protocol.libomv.LibomvProtocolRuntime
import org.hostess.protocol.libomv.ProtocolLibomvModule

class AndroidCompatibilityProbe {
    fun run(): AndroidCompatibilityResult {
        val targetSet = TargetSelectionService().emptyTargetSet(probeGroups())
        val selected = TargetSelectionService().addTarget(targetSet, GroupDisplayName("Probe Hosts"))
        val selectedTargetSet = when (selected) {
            is TargetSelectionResult.Changed -> selected.targetSet
            else -> return AndroidCompatibilityResult.androidGap("core target selection failed")
        }

        val draft = NoticeDraft(
            subject = "Android compatibility probe",
            message = "No live send.",
            targetSet = selectedTargetSet,
        )
        val coreCompile = draft.validateForSend() == NoticeDraftValidation.Valid
        val adapterLoad = loadProtocolRuntime(ProtocolLibomvModule.liveRuntime())

        return if (coreCompile && adapterLoad) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap("core compile or adapter load probe failed")
        }
    }

    private fun loadProtocolRuntime(runtime: LibomvProtocolRuntime): Boolean = listOf(
        runtime.clientSession::class.java,
        runtime.sessionPort::class.java,
        runtime.groupPort::class.java,
        runtime.inventoryPort::class.java,
        runtime.noticePort::class.java,
    ).all { it.name.isNotBlank() }

    private fun probeGroups(): List<GroupMembership> = listOf(
        GroupMembership(
            groupId = GroupId("android-probe-group"),
            displayName = GroupDisplayName("Probe Hosts"),
            canSendNotices = true,
            acceptsNotices = true,
        ),
    )
}

data class AndroidCompatibilityResult(
    val status: String,
    val coreCompile: Boolean,
    val adapterLoad: Boolean,
    val forbiddenApiScan: String,
    val blockedReason: String?,
) {
    companion object {
        fun passed(): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "passed",
            coreCompile = true,
            adapterLoad = true,
            forbiddenApiScan = "external_guard_required",
            blockedReason = null,
        )

        fun androidGap(reason: String): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = false,
            adapterLoad = false,
            forbiddenApiScan = "external_guard_required",
            blockedReason = reason,
        )
    }
}
