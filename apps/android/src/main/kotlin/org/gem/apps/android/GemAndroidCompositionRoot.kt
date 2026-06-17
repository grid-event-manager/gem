package org.gem.apps.android

import android.content.Context
import java.io.File
import java.nio.file.Path
import org.gem.core.appearance.AppearanceProfileService
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.ports.ClockPort
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.DefaultRedactionPort
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeConfirmationService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.core.theme.ThemePreferenceService
import org.gem.credential.vault.GemVaultRuntimeAccess
import org.gem.protocol.libomv.ProtocolLibomvModule
import org.gem.protocol.libomv.liveRuntimeForAndroid
import org.gem.protocol.libomv.runtime.CredentialVaultLoginSecretResolver
import org.gem.protocol.libomv.runtime.LoginSecretResolver
import org.gem.ui.design.AndroidPlatformFontCatalogue
import org.gem.ui.design.AndroidPlatformFontFamilyResolver
import org.gem.ui.runtime.GemLoginComplianceProvider
import org.gem.ui.runtime.GemUiRuntime

object GemAndroidCompositionRoot {
    fun create(context: Context): GemUiRuntime =
        create(
            androidContext = context,
            appFilesDir = context.filesDir,
        )

    internal fun create(appFilesDir: File): GemUiRuntime =
        create(
            androidContext = null,
            appFilesDir = appFilesDir,
        )

    private fun create(
        androidContext: Context?,
        appFilesDir: File,
    ): GemUiRuntime =
        GemRuntimeComposition.create(
            androidContext = androidContext,
            vaultAccess = GemAndroidVaultComposition.open(appFilesDir),
            themePreferenceService = GemAndroidPreferenceComposition.open(appFilesDir),
            appearanceProfileService = GemAndroidPreferenceComposition.openAppearanceProfiles(appFilesDir),
            lastLoginProfilePreferenceService = GemAndroidPreferenceComposition.openLastLoginProfile(appFilesDir),
            inventorySnapshotCacheDirectory = GemAndroidPreferenceComposition.inventorySnapshotCacheDirectory(appFilesDir),
        )

    private object GemRuntimeComposition {
        fun create(
            androidContext: Context?,
            vaultAccess: GemVaultRuntimeAccess,
            themePreferenceService: ThemePreferenceService,
            appearanceProfileService: AppearanceProfileService,
            lastLoginProfilePreferenceService: LastLoginProfilePreferenceService,
            inventorySnapshotCacheDirectory: Path,
        ): GemUiRuntime {
            val secretResolver = vaultAccess.loginSecretResolver()
            val protocolRuntime = if (androidContext == null) {
                ProtocolLibomvModule.liveRuntime(
                    secretResolver = secretResolver,
                    inventorySnapshotCacheDirectory = inventorySnapshotCacheDirectory,
                )
            } else {
                ProtocolLibomvModule.liveRuntimeForAndroid(
                    context = androidContext,
                    secretResolver = secretResolver,
                    inventorySnapshotCacheDirectory = inventorySnapshotCacheDirectory,
                )
            }
            val groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort)
            return GemUiRuntime(
                credentialRuntimeState = vaultAccess.credentialRuntimeState,
                clockPort = AndroidAppClockPort,
                sessionService = SessionService(
                    sessionPort = protocolRuntime.sessionPort,
                    loginComplianceService = LoginComplianceService(),
                    redactionPort = DefaultRedactionPort,
                ),
                avatarReadinessService = AvatarReadinessService(protocolRuntime.avatarPort),
                groupDirectoryService = groupDirectoryService,
                targetSelectionService = TargetSelectionService(),
                inventoryDirectoryService = InventoryDirectoryService(protocolRuntime.inventoryPort),
                inventorySelectionService = InventorySelectionService(),
                attachmentService = AttachmentService(protocolRuntime.inventoryPort),
                noticeDraftService = NoticeDraftService(),
                noticeDispatchService = NoticeDispatchService(
                    noticePort = protocolRuntime.noticePort,
                    clockPort = AndroidAppClockPort,
                ),
                noticeConfirmationService = NoticeConfirmationService(groupDirectoryService),
                loginComplianceProvider = GemUiLoginComplianceProvider,
                themePreferenceService = themePreferenceService,
                appearanceProfileService = appearanceProfileService,
                platformFontCatalogue = AndroidPlatformFontCatalogue(),
                platformFontFamilyResolver = AndroidPlatformFontFamilyResolver(),
                lastLoginProfilePreferenceService = lastLoginProfilePreferenceService,
            )
        }

        private fun GemVaultRuntimeAccess.loginSecretResolver(): LoginSecretResolver =
            credentialVault?.let(::CredentialVaultLoginSecretResolver) ?: LoginSecretResolver.unavailable()
    }

    private object GemUiLoginComplianceProvider : GemLoginComplianceProvider {
        override fun requestFor(profile: SavedAccountProfile): LoginComplianceRequest =
            LoginComplianceRequest(
                proofAccountAttested = true,
                automatedUse = true,
                scriptedAgentAttested = true,
                operatorLabel = OperatorLabel("Grid Event Manager Android"),
                proofAccountLabel = profile.label,
                evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
            )
    }

    private object AndroidAppClockPort : ClockPort {
        override fun now(): GemInstant =
            GemInstant(System.currentTimeMillis())

        override fun pause(duration: GemDelay) {
            Thread.sleep(duration.milliseconds)
        }
    }
}
