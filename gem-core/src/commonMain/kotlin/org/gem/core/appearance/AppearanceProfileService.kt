package org.gem.core.appearance

class AppearanceProfileService(
    private val store: AppearanceProfileStore,
) {
    fun loadState(): AppearanceProfileLoadResult =
        when (val loaded = store.load()) {
            is AppearanceProfileStoreLoadResult.Loaded -> AppearanceProfileLoadResult.Loaded(
                stateFrom(
                    snapshot = loaded.snapshot,
                    warning = null,
                ),
            )
            AppearanceProfileStoreLoadResult.Missing -> AppearanceProfileLoadResult.Loaded(
                stateFrom(
                    snapshot = emptySnapshot(),
                    warning = null,
                ),
            )
            is AppearanceProfileStoreLoadResult.Invalid -> AppearanceProfileLoadResult.Loaded(
                stateFrom(
                    snapshot = emptySnapshot(),
                    warning = AppearanceProfileWarning.CustomProfileStoreRejected(loaded.reason),
                ),
            )
            is AppearanceProfileStoreLoadResult.StorageFailed -> AppearanceProfileLoadResult.StorageFailed(
                state = stateFrom(
                    snapshot = emptySnapshot(),
                    warning = null,
                ),
                message = loaded.message,
            )
        }

    fun selectProfile(profileId: AppearanceProfileId): AppearanceProfileSelectionResult {
        val snapshot = loadMutableSnapshotOrReturnStorageFailure {
            return AppearanceProfileSelectionResult.StorageFailed(it)
        }
        val state = stateFrom(snapshot, warning = null)
        val profile = state.profiles.firstOrNull { it.id == profileId }
            ?: return AppearanceProfileSelectionResult.ProfileMissing(profileId)
        val nextSnapshot = snapshot.withActiveProfile(profile.mode, profile.id)

        return when (val saved = store.save(nextSnapshot)) {
            AppearanceProfileStoreSaveResult.Saved -> AppearanceProfileSelectionResult.Selected(
                state = stateFrom(nextSnapshot, warning = null),
                profile = profile,
            )
            is AppearanceProfileStoreSaveResult.StorageFailed -> AppearanceProfileSelectionResult.StorageFailed(
                saved.message,
            )
        }
    }

    fun saveProfile(
        name: AppearanceProfileName,
        mode: AppearanceMode,
        draft: AppearanceDraft,
    ): AppearanceProfileSaveResult {
        if (draft.mode != mode) {
            return AppearanceProfileSaveResult.Rejected("Draft mode does not match profile mode.")
        }

        val snapshot = loadMutableSnapshotOrReturnStorageFailure {
            return AppearanceProfileSaveResult.StorageFailed(it)
        }
        val trimmedName = name.value
        val existing = snapshot.customProfiles.firstOrNull {
            it.mode == mode && it.name.value.equals(trimmedName, ignoreCase = true)
        }
        val profileId = existing?.id ?: nextCustomProfileId(trimmedName, mode, snapshot.customProfiles)
        val profile = AppearanceProfile(
            id = profileId,
            name = AppearanceProfileName(trimmedName),
            mode = mode,
            source = AppearanceProfileSource.CUSTOM,
            textFonts = draft.textFonts,
            textColors = draft.textColors,
            elementColors = draft.elementColors,
        )
        val nextProfiles = snapshot.customProfiles
            .filterNot { it.id == profileId }
            .plus(profile)
        val nextSnapshot = snapshot
            .copy(customProfiles = nextProfiles)
            .withActiveProfile(mode, profileId)

        return when (val saved = store.save(nextSnapshot)) {
            AppearanceProfileStoreSaveResult.Saved -> AppearanceProfileSaveResult.Saved(
                state = stateFrom(nextSnapshot, warning = null),
                profile = profile,
            )
            is AppearanceProfileStoreSaveResult.StorageFailed -> AppearanceProfileSaveResult.StorageFailed(
                saved.message,
            )
        }
    }

    fun resetMode(mode: AppearanceMode): AppearanceProfileResetResult {
        val snapshot = loadMutableSnapshotOrReturnStorageFailure {
            return AppearanceProfileResetResult.StorageFailed(it)
        }
        val nextSnapshot = snapshot.withActiveProfile(mode, profileId = null)

        return when (val saved = store.save(nextSnapshot)) {
            AppearanceProfileStoreSaveResult.Saved -> {
                val state = stateFrom(nextSnapshot, warning = null)
                AppearanceProfileResetResult.Reset(
                    state = state,
                )
            }
            is AppearanceProfileStoreSaveResult.StorageFailed -> AppearanceProfileResetResult.StorageFailed(
                saved.message,
            )
        }
    }

    fun switchModePreservingProfileFamily(
        targetMode: AppearanceMode,
        sourceProfileId: AppearanceProfileId?,
    ): AppearanceProfileModeSwitchResult {
        val snapshot = loadMutableSnapshotOrReturnStorageFailure {
            return AppearanceProfileModeSwitchResult.StorageFailed(it)
        }
        val state = stateFrom(snapshot, warning = null)
        val sourceProfile = sourceProfileId
            ?.let { id -> state.profiles.firstOrNull { it.id == id } }
            ?: return AppearanceProfileModeSwitchResult.Switched(state)
        val targetProfileId = counterpartProfileId(
            targetMode = targetMode,
            sourceProfile = sourceProfile,
            profiles = state.profiles,
        )
        val nextSnapshot = snapshot.withActiveProfile(targetMode, targetProfileId)

        return when (val saved = store.save(nextSnapshot)) {
            AppearanceProfileStoreSaveResult.Saved -> AppearanceProfileModeSwitchResult.Switched(
                state = stateFrom(nextSnapshot, warning = null),
            )
            is AppearanceProfileStoreSaveResult.StorageFailed -> AppearanceProfileModeSwitchResult.StorageFailed(
                saved.message,
            )
        }
    }

    private inline fun loadMutableSnapshotOrReturnStorageFailure(
        onStorageFailure: (String?) -> Nothing,
    ): AppearanceProfileStoreSnapshot =
        when (val loaded = store.load()) {
            is AppearanceProfileStoreLoadResult.Loaded -> loaded.snapshot
            AppearanceProfileStoreLoadResult.Missing -> emptySnapshot()
            is AppearanceProfileStoreLoadResult.Invalid -> emptySnapshot()
            is AppearanceProfileStoreLoadResult.StorageFailed -> onStorageFailure(loaded.message)
        }

    private fun stateFrom(
        snapshot: AppearanceProfileStoreSnapshot,
        warning: AppearanceProfileWarning?,
    ): AppearanceProfileState {
        val stockProfiles = AppearanceProfileCatalogue.stockProfiles()
        val profiles = stockProfiles + snapshot.customProfiles

        return AppearanceProfileState(
            stockProfiles = stockProfiles,
            customProfiles = snapshot.customProfiles,
            activeLightProfileId = snapshot.activeLightProfileId,
            activeDarkProfileId = snapshot.activeDarkProfileId,
            selectedLightDraft = selectedDraftFor(
                mode = AppearanceMode.LIGHT,
                profileId = snapshot.activeLightProfileId,
                profiles = profiles,
            ),
            selectedDarkDraft = selectedDraftFor(
                mode = AppearanceMode.DARK,
                profileId = snapshot.activeDarkProfileId,
                profiles = profiles,
            ),
            warning = warning,
        )
    }

    private fun selectedDraftFor(
        mode: AppearanceMode,
        profileId: AppearanceProfileId?,
        profiles: List<AppearanceProfile>,
    ): AppearanceDraft? {
        val profile = profiles.firstOrNull { it.id == profileId && it.mode == mode }
        return profile?.let(AppearanceDraft::fromProfile)
    }

    private fun AppearanceProfileStoreSnapshot.withActiveProfile(
        mode: AppearanceMode,
        profileId: AppearanceProfileId?,
    ): AppearanceProfileStoreSnapshot =
        when (mode) {
            AppearanceMode.LIGHT -> copy(activeLightProfileId = profileId)
            AppearanceMode.DARK -> copy(activeDarkProfileId = profileId)
        }

    private fun emptySnapshot(): AppearanceProfileStoreSnapshot =
        AppearanceProfileStoreSnapshot(
            customProfiles = emptyList(),
            activeLightProfileId = null,
            activeDarkProfileId = null,
        )

    private fun counterpartProfileId(
        targetMode: AppearanceMode,
        sourceProfile: AppearanceProfile,
        profiles: List<AppearanceProfile>,
    ): AppearanceProfileId? =
        profiles
            .filter { profile ->
                profile.mode == targetMode &&
                    profile.source == sourceProfile.source &&
                    profile.id != sourceProfile.id &&
                    profile.name.value.equals(sourceProfile.name.value, ignoreCase = true)
            }
            .sortedBy { it.id.value }
            .firstOrNull()
            ?.id

    private fun nextCustomProfileId(
        name: String,
        mode: AppearanceMode,
        customProfiles: List<AppearanceProfile>,
    ): AppearanceProfileId {
        val used = customProfiles.map { it.id.value }.toSet()
        val base = "custom:${mode.name.lowercase()}:${name.slug()}"
        if (base !in used) {
            return AppearanceProfileId(base)
        }

        var suffix = 2
        while ("$base-$suffix" in used) {
            suffix += 1
        }
        return AppearanceProfileId("$base-$suffix")
    }

    private fun String.slug(): String {
        val collapsed = lowercase()
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString(separator = "")
            .replace(Regex("-+"), "-")
            .trim('-')

        return collapsed.ifBlank { "theme" }
    }
}
