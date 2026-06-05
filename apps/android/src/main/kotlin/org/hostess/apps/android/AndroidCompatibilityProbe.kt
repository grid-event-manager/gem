package org.hostess.apps.android

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.services.TargetSelectionService
import org.hostess.protocol.libomv.LibomvProtocolRuntime
import org.hostess.protocol.libomv.ProtocolLibomvModule

class AndroidCompatibilityProbe {
    fun run(): AndroidCompatibilityResult {
        val coreCompile = probeCoreCompile()
        val runtimeResult = runCatching { ProtocolLibomvModule.liveRuntime() }
        val runtime = runtimeResult.getOrNull()
        val adapterLoad = runtime?.let(::loadProtocolAdapters) ?: false
        val runtimeLoad = runtime != null && loadRuntimeClasses()
        val transportLoad = runtime != null && loadTransportClasses()

        return if (coreCompile && adapterLoad && runtimeLoad && transportLoad) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap(
                coreCompile = coreCompile,
                adapterLoad = adapterLoad,
                runtimeLoad = runtimeLoad,
                transportLoad = transportLoad,
                reason = blockedReason(coreCompile, adapterLoad, runtimeLoad, transportLoad, runtimeResult.exceptionOrNull()),
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

    private fun loadProtocolAdapters(runtime: LibomvProtocolRuntime): Boolean = listOf(
        runtime.clientSession::class.java,
        runtime.sessionPort::class.java,
        runtime.groupPort::class.java,
        runtime.inventoryPort::class.java,
        runtime.noticePort::class.java,
    ).all { it.name.isNotBlank() } && loadClasses(ADAPTER_CLASS_NAMES)

    private fun loadRuntimeClasses(): Boolean = loadClasses(RUNTIME_CLASS_NAMES)

    private fun loadTransportClasses(): Boolean = loadClasses(TRANSPORT_CLASS_NAMES)

    private fun loadClasses(classNames: List<String>): Boolean = classNames.all { className ->
        runCatching { Class.forName(className) }.isSuccess
    }

    private fun blockedReason(
        coreCompile: Boolean,
        adapterLoad: Boolean,
        runtimeLoad: Boolean,
        transportLoad: Boolean,
        failure: Throwable?,
    ): String {
        val failedLanes = listOfNotNull(
            "coreCompile".takeUnless { coreCompile },
            "adapterLoad".takeUnless { adapterLoad },
            "runtimeLoad".takeUnless { runtimeLoad },
            "transportLoad".takeUnless { transportLoad },
        )
        val cause = failure?.let { " cause=${it::class.java.simpleName}" }.orEmpty()
        return "Android compatibility probe failed lanes=${failedLanes.joinToString(",")}$cause"
    }

    private fun probeGroups(): List<GroupMembership> = listOf(
        GroupMembership.fromValues("android-probe-group", "Probe Hosts", true, true),
    )

    private companion object {
        private val ADAPTER_CLASS_NAMES = listOf(
            "org.hostess.protocol.libomv.LibomvSessionAdapter",
            "org.hostess.protocol.libomv.LibomvGroupAdapter",
            "org.hostess.protocol.libomv.LibomvInventoryAdapter",
            "org.hostess.protocol.libomv.LibomvNoticeAdapter",
        )

        private val RUNTIME_CLASS_NAMES = listOf(
            "org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime",
            "org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime",
            "org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime",
            "org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime",
            "org.hostess.protocol.libomv.runtime.AttachmentPayloadSource",
            "org.hostess.protocol.libomv.runtime.AttachmentPayloadResult",
        )

        private val TRANSPORT_CLASS_NAMES = listOf(
            "org.hostess.protocol.libomv.transport.ProtocolHttpClient",
            "org.hostess.protocol.libomv.transport.ProtocolHttpRequest",
            "org.hostess.protocol.libomv.transport.ProtocolHttpResponse",
            "org.hostess.protocol.libomv.transport.ProtocolHttpBody",
            "org.hostess.protocol.libomv.transport.OkHttpProtocolHttpClient",
        )
    }
}

data class AndroidCompatibilityResult(
    val status: String,
    val coreCompile: Boolean,
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
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
            forbiddenApiScan = "external_guard_required",
            blockedReason = null,
        )

        fun androidGap(
            coreCompile: Boolean,
            adapterLoad: Boolean,
            runtimeLoad: Boolean,
            transportLoad: Boolean,
            reason: String,
        ): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = coreCompile,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
            forbiddenApiScan = "external_guard_required",
            blockedReason = reason,
        )
    }
}
