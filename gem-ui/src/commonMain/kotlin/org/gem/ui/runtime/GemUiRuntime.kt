package org.gem.ui.runtime

import org.gem.core.appearance.AppearanceProfileService
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.CredentialService
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginProfileAuthenticationService
import org.gem.core.services.NoticeConfirmationService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SavedLoginAuthenticationService
import org.gem.core.services.SavedAccountManagementService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.core.ports.ClockPort
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.theme.ThemePreferenceService
import org.gem.ui.design.PlatformFontCatalogue
import org.gem.ui.design.PlatformFontFamilyResolver
import org.gem.ui.design.PlatformSystemFontFamilyProvider

data class GemUiRuntime(
    val credentialRuntimeState: GemCredentialRuntimeState,
    val clockPort: ClockPort,
    val sessionService: SessionService,
    val avatarReadinessService: AvatarReadinessService,
    val groupDirectoryService: GroupDirectoryService,
    val targetSelectionService: TargetSelectionService,
    val inventoryDirectoryService: InventoryDirectoryService,
    val inventorySelectionService: InventorySelectionService,
    val attachmentService: AttachmentService,
    val noticeDraftService: NoticeDraftService,
    val noticeDispatchService: NoticeDispatchService,
    val noticeConfirmationService: NoticeConfirmationService,
    val loginComplianceProvider: GemLoginComplianceProvider,
    val themePreferenceService: ThemePreferenceService,
    val appearanceProfileService: AppearanceProfileService,
    val platformFontCatalogue: PlatformFontCatalogue,
    val platformFontFamilyResolver: PlatformFontFamilyResolver,
    val platformSystemFontFamilyProvider: PlatformSystemFontFamilyProvider,
    val lastLoginProfilePreferenceService: LastLoginProfilePreferenceService,
) {
    fun credentialServiceOrNull(): CredentialService? =
        (credentialRuntimeState as? GemCredentialRuntimeReady)?.credentialService

    fun savedLoginAuthenticationServiceOrNull(): SavedLoginAuthenticationService? =
        credentialServiceOrNull()?.let { credentialService ->
            SavedLoginAuthenticationService(credentialService, sessionService)
        }

    fun loginProfileAuthenticationServiceOrNull(): LoginProfileAuthenticationService? =
        credentialServiceOrNull()?.let { credentialService ->
            LoginProfileAuthenticationService(
                credentialService = credentialService,
                savedLoginAuthenticationService = SavedLoginAuthenticationService(
                    credentialService = credentialService,
                    sessionService = sessionService,
                ),
            )
        }

    fun savedAccountManagementServiceOrNull(): SavedAccountManagementService? =
        credentialServiceOrNull()?.let(::SavedAccountManagementService)
}
