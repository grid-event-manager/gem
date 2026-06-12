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
        val vaultRuntimeLoad = probeVaultRuntimeLoad()
        val protocolAdapterLoad = runtimeLoad && transportLoad
        val loginComplianceLoad = probeLoginComplianceLoad()
        val loginPackageSerializationLoad = probeLoginPackageSerializationLoad()
        val inventoryCapabilityLoad = probeInventoryCapabilityLoad()
        val noticeProtocolLoad = probeNoticeProtocolLoad()
        val avatarReadinessLoad = loadState?.avatarLoad == true && probeAvatarReadinessLoad()

        return if (
            coreCompile &&
            adapterLoad &&
            runtimeLoad &&
            transportLoad &&
            vaultRuntimeLoad &&
            protocolAdapterLoad &&
            loginComplianceLoad &&
            loginPackageSerializationLoad &&
            inventoryCapabilityLoad &&
            noticeProtocolLoad &&
            avatarReadinessLoad
        ) {
            AndroidCompatibilityResult.passed()
        } else {
            AndroidCompatibilityResult.androidGap(
                coreCompile = coreCompile,
                adapterLoad = adapterLoad,
                runtimeLoad = runtimeLoad,
                transportLoad = transportLoad,
                vaultRuntimeLoad = vaultRuntimeLoad,
                protocolAdapterLoad = protocolAdapterLoad,
                loginComplianceLoad = loginComplianceLoad,
                loginPackageSerializationLoad = loginPackageSerializationLoad,
                inventoryCapabilityLoad = inventoryCapabilityLoad,
                noticeProtocolLoad = noticeProtocolLoad,
                avatarReadinessLoad = avatarReadinessLoad,
                reason = blockedReason(
                    coreCompile,
                    adapterLoad,
                    runtimeLoad,
                    transportLoad,
                    vaultRuntimeLoad,
                    protocolAdapterLoad,
                    loginComplianceLoad,
                    loginPackageSerializationLoad,
                    inventoryCapabilityLoad,
                    noticeProtocolLoad,
                    avatarReadinessLoad,
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

    private fun probeLoginComplianceLoad(): Boolean =
        listOf(
            HostessViewerIdentityProvider::class.java,
            LoginComplianceService::class.java,
            NoticeDispatchService::class.java,
        ).all { it.name.isNotBlank() }

    private fun probeVaultRuntimeLoad(): Boolean =
        VAULT_RUNTIME_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeLoginPackageSerializationLoad(): Boolean =
        LOGIN_PACKAGE_SERIALIZATION_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeInventoryCapabilityLoad(): Boolean =
        INVENTORY_CAPABILITY_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeNoticeProtocolLoad(): Boolean =
        NOTICE_PROTOCOL_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun probeAvatarReadinessLoad(): Boolean =
        AVATAR_READINESS_CLASSES.all { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader).name.isNotBlank() }
                .getOrDefault(false)
        }

    private fun blockedReason(
        coreCompile: Boolean,
        adapterLoad: Boolean,
        runtimeLoad: Boolean,
        transportLoad: Boolean,
        vaultRuntimeLoad: Boolean,
        protocolAdapterLoad: Boolean,
        loginComplianceLoad: Boolean,
        loginPackageSerializationLoad: Boolean,
        inventoryCapabilityLoad: Boolean,
        noticeProtocolLoad: Boolean,
        avatarReadinessLoad: Boolean,
        failure: Throwable?,
    ): String {
        val failedLanes = listOfNotNull(
            "coreCompile".takeUnless { coreCompile },
            "adapterLoad".takeUnless { adapterLoad },
            "runtimeLoad".takeUnless { runtimeLoad },
            "transportLoad".takeUnless { transportLoad },
            "vaultRuntimeLoad".takeUnless { vaultRuntimeLoad },
            "protocolAdapterLoad".takeUnless { protocolAdapterLoad },
            "loginComplianceLoad".takeUnless { loginComplianceLoad },
            "loginPackageSerializationLoad".takeUnless { loginPackageSerializationLoad },
            "inventoryCapabilityLoad".takeUnless { inventoryCapabilityLoad },
            "noticeProtocolLoad".takeUnless { noticeProtocolLoad },
            "avatarReadinessLoad".takeUnless { avatarReadinessLoad },
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

        val VAULT_RUNTIME_CLASSES = listOf(
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
        val LOGIN_PACKAGE_SERIALIZATION_CLASSES = listOf(
            "org.hostess.protocol.libomv.runtime.LoginPackageBuilder",
            "org.hostess.protocol.libomv.runtime.LoginPackageSerializer",
            "org.hostess.protocol.libomv.runtime.SecondLifePasswordHash",
            "org.hostess.protocol.libomv.runtime.HostessMachineIdentityProvider",
            "org.hostess.protocol.libomv.runtime.DefaultHostessMachineIdentityProvider",
        )
        val INVENTORY_CAPABILITY_CLASSES = listOf(
            "org.hostess.core.services.InventoryDirectoryService",
            "org.hostess.core.domain.InventoryItemDescriptor",
            "org.hostess.core.domain.InventoryItemQuery",
            "org.hostess.protocol.libomv.mapping.LoginInventoryRoots",
            "org.hostess.protocol.libomv.transport.ProtocolCapabilitySeedClient",
            "org.hostess.protocol.libomv.transport.ProtocolCapabilityCacheProvider",
            "org.hostess.protocol.libomv.runtime.ProtocolInventoryHttpSource",
            "org.hostess.protocol.libomv.mapping.LibomvInventoryItemMapping",
        )
        val NOTICE_PROTOCOL_CLASSES = listOf(
            "org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient",
            "org.hostess.protocol.libomv.transport.LibomvNoticePacketCodec",
            "org.hostess.protocol.libomv.runtime.ProtocolNoticeCircuitSource",
        )
        val AVATAR_READINESS_CLASSES = listOf(
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
    val vaultRuntimeLoad: Boolean,
    val protocolAdapterLoad: Boolean,
    val loginComplianceLoad: Boolean,
    val loginPackageSerializationLoad: Boolean,
    val inventoryCapabilityLoad: Boolean,
    val noticeProtocolLoad: Boolean,
    val avatarReadinessLoad: Boolean,
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
            vaultRuntimeLoad = true,
            protocolAdapterLoad = true,
            loginComplianceLoad = true,
            loginPackageSerializationLoad = true,
            inventoryCapabilityLoad = true,
            noticeProtocolLoad = true,
            avatarReadinessLoad = true,
            noLiveGridContact = true,
            noUiSurface = true,
            blockedReason = null,
        )

        fun androidGap(
            coreCompile: Boolean,
            adapterLoad: Boolean,
            runtimeLoad: Boolean,
            transportLoad: Boolean,
            vaultRuntimeLoad: Boolean,
            protocolAdapterLoad: Boolean,
            loginComplianceLoad: Boolean,
            loginPackageSerializationLoad: Boolean,
            inventoryCapabilityLoad: Boolean,
            noticeProtocolLoad: Boolean,
            avatarReadinessLoad: Boolean,
            reason: String,
        ): AndroidCompatibilityResult = AndroidCompatibilityResult(
            status = "android_gap",
            coreCompile = coreCompile,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
            vaultRuntimeLoad = vaultRuntimeLoad,
            protocolAdapterLoad = protocolAdapterLoad,
            loginComplianceLoad = loginComplianceLoad,
            loginPackageSerializationLoad = loginPackageSerializationLoad,
            inventoryCapabilityLoad = inventoryCapabilityLoad,
            noticeProtocolLoad = noticeProtocolLoad,
            avatarReadinessLoad = avatarReadinessLoad,
            noLiveGridContact = true,
            noUiSurface = true,
            blockedReason = reason,
        )
    }
}
