package org.hostess.apps.android

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.protocol.libomv.ProtocolLibomvModule
import org.hostess.protocol.libomv.runtime.HostessViewerIdentityProvider
import org.hostess.protocol.libomv.runtime.LoginSecretResolver

class AndroidCompatibilityProbe {
    fun run(): AndroidCompatibilityResult {
        val coreCompile = probeCoreCompile()
        val runtimeResult = runCatching { defaultProtocolLoad() }
        val loadState = runtimeResult.getOrNull()
        val adapterLoad = loadState?.adapterLoad ?: false
        val runtimeLoad = loadState?.runtimeLoad ?: false
        val transportLoad = loadState?.transportLoad ?: false
        val trackAVaultLoad = probeTrackAVaultLoad()
        val trackCClassLoad = runtimeLoad && transportLoad
        val trackDComplianceLoad = probeTrackDComplianceLoad()
        val trackDsLoginPackageLoad = probeTrackDsLoginPackageLoad()
        val trackGGridLoad = probeTrackGGridLoad()
        val trackHNoticeLoad = probeTrackHNoticeLoad()
        val trackJAvatarLoad = loadState?.avatarLoad == true && probeTrackJAvatarLoad()

        return if (
            coreCompile &&
            adapterLoad &&
            runtimeLoad &&
            transportLoad &&
            trackAVaultLoad &&
            trackCClassLoad &&
            trackDComplianceLoad &&
            trackDsLoginPackageLoad &&
            trackGGridLoad &&
            trackHNoticeLoad &&
            trackJAvatarLoad
        ) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap(
                coreCompile = coreCompile,
                adapterLoad = adapterLoad,
                runtimeLoad = runtimeLoad,
                transportLoad = transportLoad,
                trackAVaultLoad = trackAVaultLoad,
                trackCClassLoad = trackCClassLoad,
                trackDComplianceLoad = trackDComplianceLoad,
                trackDsLoginPackageLoad = trackDsLoginPackageLoad,
                trackGGridLoad = trackGGridLoad,
                trackHNoticeLoad = trackHNoticeLoad,
                trackJAvatarLoad = trackJAvatarLoad,
                reason = blockedReason(
                    coreCompile,
                    adapterLoad,
                    runtimeLoad,
                    transportLoad,
                    trackAVaultLoad,
                    trackCClassLoad,
                    trackDComplianceLoad,
                    trackDsLoginPackageLoad,
                    trackGGridLoad,
                    trackHNoticeLoad,
                    trackJAvatarLoad,
                    runtimeResult.exceptionOrNull(),
                ),
            )
        }
    }

    private fun probeCoreCompile(): Boolean {
        val selected = GroupTargetSet.from(probeGroups()).addAllSendable()
        val selectedTargetSet = when (selected) {
            is TargetSelectionResult.Changed -> selected.targetSet
            else -> return false
        }

        val draft = NoticeDraft(
            subject = "Android compatibility probe",
            message = "No live send.",
            targetSet = selectedTargetSet,
            attachments = listOf(
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("android-probe-landmark")),
            ),
        )
        return draft.validateForSend() == NoticeDraftValidation.Valid
    }

    private fun probeTrackDComplianceLoad(): Boolean =
        listOf(
            HostessViewerIdentityProvider::class.java,
            LoginComplianceService::class.java,
            NoticeDispatchService::class.java,
        ).all { it.name.isNotBlank() }

    private fun probeTrackAVaultLoad(): Boolean =
        TRACK_A_VAULT_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeTrackDsLoginPackageLoad(): Boolean =
        TRACK_DS_LOGIN_PACKAGE_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeTrackGGridLoad(): Boolean =
        TRACK_G_GRID_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeTrackHNoticeLoad(): Boolean =
        TRACK_H_NOTICE_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeTrackJAvatarLoad(): Boolean =
        TRACK_J_AVATAR_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun blockedReason(
        coreCompile: Boolean,
        adapterLoad: Boolean,
        runtimeLoad: Boolean,
        transportLoad: Boolean,
        trackAVaultLoad: Boolean,
        trackCClassLoad: Boolean,
        trackDComplianceLoad: Boolean,
        trackDsLoginPackageLoad: Boolean,
        trackGGridLoad: Boolean,
        trackHNoticeLoad: Boolean,
        trackJAvatarLoad: Boolean,
        failure: Throwable?,
    ): String {
        val failedLanes = listOfNotNull(
            "coreCompile".takeUnless { coreCompile },
            "adapterLoad".takeUnless { adapterLoad },
            "runtimeLoad".takeUnless { runtimeLoad },
            "transportLoad".takeUnless { transportLoad },
            "trackAVaultLoad".takeUnless { trackAVaultLoad },
            "trackCClassLoad".takeUnless { trackCClassLoad },
            "trackDComplianceLoad".takeUnless { trackDComplianceLoad },
            "trackDsLoginPackageLoad".takeUnless { trackDsLoginPackageLoad },
            "trackGGridLoad".takeUnless { trackGGridLoad },
            "trackHNoticeLoad".takeUnless { trackHNoticeLoad },
            "trackJAvatarLoad".takeUnless { trackJAvatarLoad },
        )
        val cause = failure?.let { " cause=${it::class.java.simpleName}" }.orEmpty()
        return "Android compatibility probe failed lanes=${failedLanes.joinToString(",")}$cause"
    }

    private fun probeGroups(): List<GroupMembership> = listOf(
        GroupMembership.fromValues("android-probe-group", "Probe Hosts", true, true),
    )

    private companion object {
        fun defaultProtocolLoad(): AndroidProtocolLoadState {
            val runtime = ProtocolLibomvModule.liveRuntime(LoginSecretResolver.unavailable())
            val avatarService = AvatarReadinessService(runtime.avatarPort)
            val loadState = runtime.loadState
            return AndroidProtocolLoadState(
                adapterLoad = loadState.adapterLoad,
                runtimeLoad = loadState.runtimeLoad,
                transportLoad = loadState.transportLoad,
                avatarLoad = avatarService::class.java.name.isNotBlank(),
            )
        }

        val TRACK_A_VAULT_CLASSES = listOf(
            "org.hostess.apps.android.HostessAndroidVaultComposition",
            "org.hostess.core.services.HostessCredentialRuntimeState",
            "org.hostess.core.services.HostessCredentialRuntimeReady",
            "org.hostess.core.services.HostessCredentialRuntimeUnavailable",
            "org.hostess.core.services.HostessCredentialRuntimeResetRequired",
            "org.hostess.credential.vault.VaultAccessService",
            "org.hostess.credential.vault.VaultCredentialRuntimeStateMapper",
            "org.hostess.credential.vault.AndroidKeystoreVaultKeySource",
            "org.hostess.credential.vault.EncryptedHostessVault",
            "org.hostess.credential.vault.VaultFileAccountProfileStore",
            "org.hostess.protocol.libomv.runtime.CredentialVaultLoginSecretResolver",
        )
        val TRACK_DS_LOGIN_PACKAGE_CLASSES = listOf(
            "org.hostess.protocol.libomv.runtime.LoginPackageBuilder",
            "org.hostess.protocol.libomv.runtime.LoginPackageSerializer",
            "org.hostess.protocol.libomv.runtime.SecondLifePasswordHash",
            "org.hostess.protocol.libomv.runtime.HostessMachineIdentityProvider",
            "org.hostess.protocol.libomv.runtime.DefaultHostessMachineIdentityProvider",
        )
        val TRACK_G_GRID_CLASSES = listOf(
            "org.hostess.core.services.InventoryDirectoryService",
            "org.hostess.core.domain.InventoryItemDescriptor",
            "org.hostess.core.domain.InventoryItemQuery",
            "org.hostess.protocol.libomv.mapping.LoginInventoryRoots",
            "org.hostess.protocol.libomv.transport.ProtocolCapabilitySeedClient",
            "org.hostess.protocol.libomv.transport.ProtocolCapabilityCacheProvider",
            "org.hostess.protocol.libomv.runtime.ProtocolInventoryHttpSource",
            "org.hostess.protocol.libomv.mapping.LibomvInventoryItemMapping",
        )
        val TRACK_H_NOTICE_CLASSES = listOf(
            "org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient",
            "org.hostess.protocol.libomv.transport.LibomvNoticePacketCodec",
            "org.hostess.protocol.libomv.runtime.ProtocolNoticeCircuitSource",
        )
        val TRACK_J_AVATAR_CLASSES = listOf(
            "org.hostess.core.services.AvatarReadinessService",
            "org.hostess.core.ports.AvatarPort",
            "org.hostess.core.ports.AvatarReadinessProof",
            "org.hostess.protocol.libomv.LibomvAvatarAdapter",
            "org.hostess.protocol.libomv.runtime.ProtocolAvatarRuntime",
            "org.hostess.protocol.libomv.runtime.ProtocolAvatarAppearanceSource",
        )
    }
}

internal data class AndroidProtocolLoadState(
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
    val avatarLoad: Boolean,
)

data class AndroidCompatibilityResult(
    val status: String,
    val coreCompile: Boolean,
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
    val trackAVaultLoad: Boolean,
    val trackCClassLoad: Boolean,
    val trackDComplianceLoad: Boolean,
    val trackDsLoginPackageLoad: Boolean,
    val trackGGridLoad: Boolean,
    val trackHNoticeLoad: Boolean,
    val trackJAvatarLoad: Boolean,
    val noLiveGridContact: Boolean,
    val noUiSurface: Boolean,
    val blockedReason: String?,
) {
    companion object {
        fun passed(): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "passed",
            coreCompile = true,
            adapterLoad = true,
            runtimeLoad = true,
            transportLoad = true,
            trackAVaultLoad = true,
            trackCClassLoad = true,
            trackDComplianceLoad = true,
            trackDsLoginPackageLoad = true,
            trackGGridLoad = true,
            trackHNoticeLoad = true,
            trackJAvatarLoad = true,
            noLiveGridContact = true,
            noUiSurface = true,
            blockedReason = null,
        )

        fun androidGap(
            coreCompile: Boolean,
            adapterLoad: Boolean,
            runtimeLoad: Boolean,
            transportLoad: Boolean,
            trackAVaultLoad: Boolean,
            trackCClassLoad: Boolean,
            trackDComplianceLoad: Boolean,
            trackDsLoginPackageLoad: Boolean,
            trackGGridLoad: Boolean,
            trackHNoticeLoad: Boolean,
            trackJAvatarLoad: Boolean,
            reason: String,
        ): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = coreCompile,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
            trackAVaultLoad = trackAVaultLoad,
            trackCClassLoad = trackCClassLoad,
            trackDComplianceLoad = trackDComplianceLoad,
            trackDsLoginPackageLoad = trackDsLoginPackageLoad,
            trackGGridLoad = trackGGridLoad,
            trackHNoticeLoad = trackHNoticeLoad,
            trackJAvatarLoad = trackJAvatarLoad,
            noLiveGridContact = true,
            noUiSurface = true,
            blockedReason = reason,
        )
    }
}
