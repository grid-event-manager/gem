package org.hostess.apps.android

import android.content.Context
import java.io.File
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.core.ports.ClockPort
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.DefaultRedactionPort
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.credential.vault.HostessVaultRuntimeAccess
import org.hostess.protocol.libomv.ProtocolLibomvModule
import org.hostess.protocol.libomv.runtime.CredentialVaultLoginSecretResolver
import org.hostess.protocol.libomv.runtime.LoginSecretResolver
import org.hostess.ui.runtime.HostessLoginComplianceProvider
import org.hostess.ui.runtime.HostessUiRuntime

object HostessAndroidCompositionRoot {
    fun create(context: Context): HostessUiRuntime =
        create(context.filesDir)

    internal fun create(appFilesDir: File): HostessUiRuntime =
        HostessRuntimeComposition.create(HostessAndroidVaultComposition.open(appFilesDir))

    private object HostessRuntimeComposition {
        fun create(vaultAccess: HostessVaultRuntimeAccess): HostessUiRuntime {
            val protocolRuntime = ProtocolLibomvModule.liveRuntime(vaultAccess.loginSecretResolver())
            return HostessUiRuntime(
                credentialRuntimeState = vaultAccess.credentialRuntimeState,
                sessionService = SessionService(
                    sessionPort = protocolRuntime.sessionPort,
                    loginComplianceService = LoginComplianceService(),
                    redactionPort = DefaultRedactionPort,
                ),
                avatarReadinessService = AvatarReadinessService(protocolRuntime.avatarPort),
                groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort),
                targetSelectionService = TargetSelectionService(),
                inventoryDirectoryService = InventoryDirectoryService(protocolRuntime.inventoryPort),
                inventorySelectionService = InventorySelectionService(),
                attachmentService = AttachmentService(protocolRuntime.inventoryPort),
                noticeDraftService = NoticeDraftService(),
                noticeDispatchService = NoticeDispatchService(
                    noticePort = protocolRuntime.noticePort,
                    clockPort = AndroidAppClockPort,
                ),
                loginComplianceProvider = HostessUiLoginComplianceProvider,
            )
        }

        private fun HostessVaultRuntimeAccess.loginSecretResolver(): LoginSecretResolver =
            credentialVault?.let(::CredentialVaultLoginSecretResolver) ?: LoginSecretResolver.unavailable()
    }

    private object HostessUiLoginComplianceProvider : HostessLoginComplianceProvider {
        override fun requestFor(profile: SavedAccountProfile): LoginComplianceRequest =
            LoginComplianceRequest(
                proofAccountAttested = true,
                automatedUse = true,
                scriptedAgentAttested = true,
                operatorLabel = OperatorLabel("Hostess Android"),
                proofAccountLabel = profile.label,
                evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
            )
    }

    private object AndroidAppClockPort : ClockPort {
        override fun now(): HostessInstant =
            HostessInstant(System.currentTimeMillis())

        override fun pause(duration: HostessDelay) {
            Thread.sleep(duration.milliseconds)
        }
    }
}
