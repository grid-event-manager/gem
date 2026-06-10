package org.hostess.ui.runtime

import org.hostess.core.services.AttachmentService
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.CredentialService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SavedLoginAuthenticationService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService

data class HostessUiRuntime(
    val credentialRuntimeState: HostessCredentialRuntimeState,
    val sessionService: SessionService,
    val avatarReadinessService: AvatarReadinessService,
    val groupDirectoryService: GroupDirectoryService,
    val targetSelectionService: TargetSelectionService,
    val inventoryDirectoryService: InventoryDirectoryService,
    val inventorySelectionService: InventorySelectionService,
    val attachmentService: AttachmentService,
    val noticeDraftService: NoticeDraftService,
    val noticeDispatchService: NoticeDispatchService,
    val loginComplianceProvider: HostessLoginComplianceProvider,
) {
    fun credentialServiceOrNull(): CredentialService? =
        (credentialRuntimeState as? HostessCredentialRuntimeReady)?.credentialService

    fun savedLoginAuthenticationServiceOrNull(): SavedLoginAuthenticationService? =
        credentialServiceOrNull()?.let { credentialService ->
            SavedLoginAuthenticationService(credentialService, sessionService)
        }
}
