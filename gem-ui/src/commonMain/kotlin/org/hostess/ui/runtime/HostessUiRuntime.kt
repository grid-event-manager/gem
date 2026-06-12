package org.hostess.ui.runtime

import org.hostess.core.services.AttachmentService
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.CredentialService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.LoginProfileAuthenticationService
import org.hostess.core.services.NoticeConfirmationService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SavedLoginAuthenticationService
import org.hostess.core.services.SavedAccountManagementService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.core.ports.ClockPort
import org.hostess.core.preferences.LastLoginProfilePreferenceService
import org.hostess.core.theme.ThemePreferenceService

data class HostessUiRuntime(
    val credentialRuntimeState: HostessCredentialRuntimeState,
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
    val loginComplianceProvider: HostessLoginComplianceProvider,
    val themePreferenceService: ThemePreferenceService,
    val lastLoginProfilePreferenceService: LastLoginProfilePreferenceService,
) {
    fun credentialServiceOrNull(): CredentialService? =
        (credentialRuntimeState as? HostessCredentialRuntimeReady)?.credentialService

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
