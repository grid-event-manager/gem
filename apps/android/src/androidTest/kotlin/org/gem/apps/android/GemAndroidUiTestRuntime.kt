package org.gem.apps.android

import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceProfileService
import org.gem.core.appearance.AppearanceProfileStore
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSaveResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SessionId
import org.gem.core.domain.SharedSecret
import org.gem.core.ports.AccountProfileIdSource
import org.gem.core.ports.AccountProfileStore
import org.gem.core.ports.AccountProfileStoreDeleteResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.ports.AccountProfileStoreSaveResult
import org.gem.core.ports.AccountProfileStoreUpdateResult
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.CredentialVault
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.ports.InventoryPort
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.NoticePort
import org.gem.core.ports.RedactionPort
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.core.ports.SessionPort
import org.gem.core.ports.SimulatorPresenceProof
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.ports.SimulatorPresenceProofStatus
import org.gem.core.preferences.LastLoginProfilePreferenceLoadResult
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.preferences.LastLoginProfilePreferenceStore
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.CredentialService
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeConfirmationService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.core.theme.ThemePreferenceService
import org.gem.core.theme.ThemePreferenceStore
import org.gem.ui.design.PlatformFontCatalogue
import org.gem.ui.design.PlatformFontFamilyResolver
import org.gem.ui.design.AndroidPlatformSystemFontFamilyProvider
import org.gem.ui.runtime.GemLoginComplianceProvider
import org.gem.ui.runtime.GemUiRuntime

internal object GemAndroidUiTestRuntime {
    fun ready(): GemUiRuntime {
        val profile = SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:android-ui"),
            loginName = loginName("venuehost resident"),
            label = "venuehost resident",
            credentialHandle = CredentialHandle("gem-vault:v1:android-ui"),
            startLocation = null,
        )
        val profileStore = AndroidUiAccountProfileStore(listOf(profile))
        val credentialVault = AndroidUiCredentialVault().apply {
            materials[profile.credentialHandle] = LoginCredentialMaterial(
                loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
                loginName = profile.loginName,
                sharedSecret = SharedSecret.fromPlainText("test-password") ?: error("invalid test password"),
                startLocation = null,
            )
        }
        val credentialService = CredentialService(
            accountProfileStore = profileStore,
            credentialVault = credentialVault,
            accountProfileIdSource = AndroidUiProfileIdSource(),
        )
        val groupDirectoryService = GroupDirectoryService(AndroidUiGroupPort())
        val inventoryPort = AndroidUiInventoryPort()
        return GemUiRuntime(
            credentialRuntimeState = GemCredentialRuntimeReady(credentialService),
            clockPort = AndroidUiClockPort,
            sessionService = SessionService(
                sessionPort = AndroidUiSessionPort,
                loginComplianceService = LoginComplianceService(),
                redactionPort = RedactionPort { value -> value },
            ),
            avatarReadinessService = AvatarReadinessService(AndroidUiAvatarPort),
            groupDirectoryService = groupDirectoryService,
            targetSelectionService = TargetSelectionService(),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDraftService = NoticeDraftService(),
            noticeDispatchService = NoticeDispatchService(AndroidUiNoticePort, AndroidUiClockPort),
            noticeConfirmationService = NoticeConfirmationService(groupDirectoryService),
            loginComplianceProvider = AndroidUiLoginComplianceProvider,
            themePreferenceService = ThemePreferenceService(AndroidUiThemePreferenceStore()),
            appearanceProfileService = AppearanceProfileService(AndroidUiAppearanceProfileStore()),
            platformFontCatalogue = PlatformFontCatalogue {
                listOf(AppearanceFontFamily("sans-serif"))
            },
            platformFontFamilyResolver = PlatformFontFamilyResolver {
                FontFamily.Default
            },
            platformSystemFontFamilyProvider = AndroidPlatformSystemFontFamilyProvider,
            lastLoginProfilePreferenceService = LastLoginProfilePreferenceService(
                AndroidUiLastLoginProfilePreferenceStore(profile.profileId),
            ),
        )
    }

    private fun loginName(value: String): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput(value)) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid test login name")
        }
}

private class AndroidUiAccountProfileStore(
    profiles: List<SavedAccountProfile>,
) : AccountProfileStore {
    private val profilesById = profiles.associateBy { it.profileId }.toMutableMap()

    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(profilesById.values.toList())

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult {
        profilesById[profile.profileId] = profile
        return AccountProfileStoreSaveResult.Saved(profile)
    }

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult =
        if (profilesById.containsKey(profile.profileId)) {
            profilesById[profile.profileId] = profile
            AccountProfileStoreUpdateResult.Updated(profile)
        } else {
            AccountProfileStoreUpdateResult.Missing(profile.profileId)
        }

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        if (profilesById.remove(profileId) == null) {
            AccountProfileStoreDeleteResult.Missing(profileId)
        } else {
            AccountProfileStoreDeleteResult.Deleted(profileId)
        }
}

private class AndroidUiCredentialVault : CredentialVault {
    val materials: MutableMap<CredentialHandle, LoginCredentialMaterial> = linkedMapOf()
    private var nextId = 0

    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult {
        nextId += 1
        val handle = CredentialHandle("gem-vault:v1:android-ui-$nextId")
        materials[handle] = material
        return CredentialVaultSaveResult.Saved(handle)
    }

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult =
        if (materials.containsKey(credentialHandle)) {
            materials[credentialHandle] = material
            CredentialVaultUpdateResult.Updated(credentialHandle)
        } else {
            CredentialVaultUpdateResult.Missing(credentialHandle)
        }

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult =
        if (materials.remove(credentialHandle) == null) {
            CredentialVaultDeleteResult.Missing(credentialHandle)
        } else {
            CredentialVaultDeleteResult.Deleted(credentialHandle)
        }

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult =
        materials[credentialHandle]?.let(CredentialVaultResolveResult::Resolved)
            ?: CredentialVaultResolveResult.Missing(credentialHandle)
}

private class AndroidUiProfileIdSource : AccountProfileIdSource {
    private var nextId = 0

    override fun nextProfileId(): AccountProfileId {
        nextId += 1
        return AccountProfileId("profile:v1:android-ui-new-$nextId")
    }
}

private object AndroidUiSessionPort : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        SessionLoginResult.Success(
            GemSession(
                sessionId = SessionId("android-ui-session"),
                accountLabel = request.accountLabel,
                startedAt = GemInstant.EPOCH,
                isActive = true,
            ),
        )

    override fun logout(session: GemSession): SessionLogoutResult =
        SessionLogoutResult.LoggedOut
}

private object AndroidUiAvatarPort : AvatarPort {
    override fun ensureReady(session: GemSession): AvatarReadinessResult =
        AvatarReadinessResult.Success(AvatarReadinessProof.success(regionName = "London City"))
}

private class AndroidUiGroupPort : GroupPort {
    override fun currentGroups(session: GemSession): GroupListResult =
        GroupListResult.Success(emptyList())

    override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult =
        SimulatorPresenceProofResult.Success(
            SimulatorPresenceProof(
                simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
                agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
            ),
        )

    override fun noticeArchive(
        session: GemSession,
        group: GroupMembership,
    ): GroupNoticeArchiveResult =
        GroupNoticeArchiveResult.Success(group, emptyList())
}

private class AndroidUiInventoryPort : InventoryPort {
    override fun resolveExistingAttachment(
        session: GemSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        AttachmentResolutionResult.Resolved(
            AttachmentRef(
                attachmentId = request.itemId,
                ownerId = AttachmentOwnerId("android-ui-owner"),
                kind = request.kind,
            ),
        )

    override fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult =
        InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), emptyList()))

    override fun listItems(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult =
        InventoryItemListResult.Success(emptyList())
}

private object AndroidUiNoticePort : NoticePort {
    override fun sendGroupNotice(
        session: GemSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus =
        GroupSendStatus(
            group = GroupMembership(
                groupId = GroupId(group.groupId.value),
                displayName = GroupDisplayName(group.displayName.value),
                canSendNotices = true,
                acceptsNotices = group.acceptsNotices,
            ),
            state = GroupSendState.SENT,
        )
}

private object AndroidUiClockPort : ClockPort {
    override fun now(): GemInstant = GemInstant.EPOCH

    override fun pause(duration: GemDelay) = Unit
}

private object AndroidUiLoginComplianceProvider : GemLoginComplianceProvider {
    override fun requestFor(profile: SavedAccountProfile): LoginComplianceRequest =
        LoginComplianceRequest(
            proofAccountAttested = true,
            automatedUse = true,
            scriptedAgentAttested = true,
            operatorLabel = OperatorLabel("Grid Event Manager Android UI test"),
            proofAccountLabel = profile.label,
            evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
        )
}

private class AndroidUiThemePreferenceStore : ThemePreferenceStore {
    private var preference: ThemePreference? = null

    override fun load(): ThemePreferenceLoadResult =
        preference?.let(ThemePreferenceLoadResult::Loaded) ?: ThemePreferenceLoadResult.Missing

    override fun save(preference: ThemePreference): ThemePreferenceSaveResult {
        this.preference = preference
        return ThemePreferenceSaveResult.Saved
    }
}

private class AndroidUiLastLoginProfilePreferenceStore(
    private var profileId: AccountProfileId?,
) : LastLoginProfilePreferenceStore {
    override fun load(): LastLoginProfilePreferenceLoadResult =
        profileId?.let(LastLoginProfilePreferenceLoadResult::Loaded) ?: LastLoginProfilePreferenceLoadResult.Missing

    override fun save(profileId: AccountProfileId): LastLoginProfilePreferenceSaveResult {
        this.profileId = profileId
        return LastLoginProfilePreferenceSaveResult.Saved
    }
}

private class AndroidUiAppearanceProfileStore : AppearanceProfileStore {
    private var snapshot: AppearanceProfileStoreSnapshot? = null

    override fun load(): AppearanceProfileStoreLoadResult =
        snapshot
            ?.let(AppearanceProfileStoreLoadResult::Loaded)
            ?: AppearanceProfileStoreLoadResult.Missing

    override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult {
        this.snapshot = snapshot
        return AppearanceProfileStoreSaveResult.Saved
    }
}
