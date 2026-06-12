package org.gem.apps.android

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeDispatchService
import org.gem.protocol.libomv.ProtocolLibomvModule
import org.gem.protocol.libomv.runtime.GemViewerIdentityProvider
import org.gem.protocol.libomv.runtime.LoginSecretResolver

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
            GemViewerIdentityProvider::class.java,
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
            "org.gem.apps.android.GemAndroidVaultComposition",
            "org.gem.core.services.GemCredentialRuntimeState",
            "org.gem.core.services.GemCredentialRuntimeReady",
            "org.gem.core.services.GemCredentialRuntimeUnavailable",
            "org.gem.core.services.GemCredentialRuntimeResetRequired",
            "org.gem.credential.vault.VaultAccessService",
            "org.gem.credential.vault.VaultCredentialRuntimeStateMapper",
            "org.gem.credential.vault.AndroidKeystoreVaultKeySource",
            "org.gem.credential.vault.EncryptedGemVault",
            "org.gem.credential.vault.VaultFileAccountProfileStore",
            "org.gem.protocol.libomv.runtime.CredentialVaultLoginSecretResolver",
        )
        val LOGIN_PACKAGE_SERIALIZATION_CLASSES = listOf(
            "org.gem.protocol.libomv.runtime.LoginPackageBuilder",
            "org.gem.protocol.libomv.runtime.LoginPackageSerializer",
            "org.gem.protocol.libomv.runtime.SecondLifePasswordHash",
            "org.gem.protocol.libomv.runtime.GemMachineIdentityProvider",
            "org.gem.protocol.libomv.runtime.DefaultGemMachineIdentityProvider",
        )
        val INVENTORY_CAPABILITY_CLASSES = listOf(
            "org.gem.core.services.InventoryDirectoryService",
            "org.gem.core.domain.InventoryItemDescriptor",
            "org.gem.core.domain.InventoryItemQuery",
            "org.gem.protocol.libomv.mapping.LoginInventoryRoots",
            "org.gem.protocol.libomv.transport.ProtocolCapabilitySeedClient",
            "org.gem.protocol.libomv.transport.ProtocolCapabilityCacheProvider",
            "org.gem.protocol.libomv.runtime.ProtocolInventoryHttpSource",
            "org.gem.protocol.libomv.mapping.LibomvInventoryItemMapping",
        )
        val NOTICE_PROTOCOL_CLASSES = listOf(
            "org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient",
            "org.gem.protocol.libomv.transport.LibomvNoticePacketCodec",
            "org.gem.protocol.libomv.runtime.ProtocolNoticeCircuitSource",
        )
        val AVATAR_READINESS_CLASSES = listOf(
            "org.gem.core.services.AvatarReadinessService",
            "org.gem.core.ports.AvatarPort",
            "org.gem.core.ports.AvatarReadinessProof",
            "org.gem.protocol.libomv.LibomvAvatarAdapter",
            "org.gem.protocol.libomv.runtime.ProtocolAvatarRuntime",
            "org.gem.protocol.libomv.runtime.ProtocolAvatarAppearanceSource",
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
