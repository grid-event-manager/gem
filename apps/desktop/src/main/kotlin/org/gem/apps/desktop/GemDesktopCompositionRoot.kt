package org.gem.apps.desktop

import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.ScriptedAgentEvidenceSource
import java.nio.file.Path
import org.gem.core.appearance.AppearanceProfileService
import org.gem.core.language.LanguagePreferenceService
import org.gem.core.ports.ClockPort
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.DefaultRedactionPort
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.core.theme.ThemePreferenceService
import org.gem.credential.vault.GemVaultRuntimeAccess
import org.gem.protocol.libomv.ProtocolLibomvModule
import org.gem.protocol.libomv.runtime.CredentialVaultLoginSecretResolver
import org.gem.protocol.libomv.runtime.LoginSecretResolver
import org.gem.ui.design.JvmPlatformFontCatalogue
import org.gem.ui.design.JvmPlatformFontFamilyResolver
import org.gem.ui.design.JvmPlatformSystemFontFamilyProvider
import org.gem.ui.design.PlatformSystemFontFamilyProvider
import org.gem.ui.runtime.GemLoginComplianceProvider
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.text.PlatformLocaleProvider

object GemDesktopCompositionRoot {
    fun create(): GemUiRuntime {
        GemDesktopStorageMigration.run()
        return GemRuntimeComposition.create(
            vaultAccess = DesktopVaultComposition.open(),
            themePreferenceService = DesktopPreferenceComposition.open(),
            languagePreferenceService = DesktopPreferenceComposition.openLanguagePreference(),
            appearanceProfileService = DesktopPreferenceComposition.openAppearanceProfiles(),
            lastLoginProfilePreferenceService = DesktopPreferenceComposition.openLastLoginProfile(),
            inventorySnapshotCacheDirectory = DesktopPreferenceComposition.inventorySnapshotCacheDirectory(),
            platformLocaleProvider = DesktopPlatformLocaleProvider(),
            platformSystemFontFamilyProvider = JvmPlatformSystemFontFamilyProvider(),
        )
    }

    internal fun create(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): GemUiRuntime {
        GemDesktopStorageMigration.run(osName, env, userHome)
        return GemRuntimeComposition.create(
            vaultAccess = DesktopVaultComposition.open(osName, env, userHome),
            themePreferenceService = DesktopPreferenceComposition.open(osName, env, userHome),
            languagePreferenceService = DesktopPreferenceComposition.openLanguagePreference(osName, env, userHome),
            appearanceProfileService = DesktopPreferenceComposition.openAppearanceProfiles(osName, env, userHome),
            lastLoginProfilePreferenceService = DesktopPreferenceComposition.openLastLoginProfile(osName, env, userHome),
            inventorySnapshotCacheDirectory = DesktopPreferenceComposition.inventorySnapshotCacheDirectory(osName, env, userHome),
            platformLocaleProvider = DesktopPlatformLocaleProvider(),
            platformSystemFontFamilyProvider = JvmPlatformSystemFontFamilyProvider(osName),
        )
    }

    private object GemRuntimeComposition {
        fun create(
            vaultAccess: GemVaultRuntimeAccess,
            themePreferenceService: ThemePreferenceService,
            languagePreferenceService: LanguagePreferenceService,
            appearanceProfileService: AppearanceProfileService,
            lastLoginProfilePreferenceService: LastLoginProfilePreferenceService,
            inventorySnapshotCacheDirectory: Path,
            platformLocaleProvider: PlatformLocaleProvider,
            platformSystemFontFamilyProvider: PlatformSystemFontFamilyProvider,
        ): GemUiRuntime {
            val protocolRuntime = ProtocolLibomvModule.liveRuntime(
                secretResolver = vaultAccess.loginSecretResolver(),
                inventorySnapshotCacheDirectory = inventorySnapshotCacheDirectory,
            )
            val groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort)
            return GemUiRuntime(
                credentialRuntimeState = vaultAccess.credentialRuntimeState,
                clockPort = DesktopAppClockPort,
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
                    clockPort = DesktopAppClockPort,
                ),
                loginComplianceProvider = GemUiLoginComplianceProvider,
                themePreferenceService = themePreferenceService,
                languagePreferenceService = languagePreferenceService,
                platformLocaleProvider = platformLocaleProvider,
                appearanceProfileService = appearanceProfileService,
                platformFontCatalogue = JvmPlatformFontCatalogue(),
                platformFontFamilyResolver = JvmPlatformFontFamilyResolver(),
                platformSystemFontFamilyProvider = platformSystemFontFamilyProvider,
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
                operatorLabel = OperatorLabel("Grid Event Manager Desktop"),
                proofAccountLabel = profile.label,
                evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
            )
    }

    private object DesktopAppClockPort : ClockPort {
        override fun now(): GemInstant =
            GemInstant(System.currentTimeMillis())

        override fun pause(duration: GemDelay) {
            Thread.sleep(duration.milliseconds)
        }
    }
}
