package org.gem.core.appearance

sealed interface AppearanceProfileStoreLoadResult {
    data class Loaded(val snapshot: AppearanceProfileStoreSnapshot) : AppearanceProfileStoreLoadResult
    data object Missing : AppearanceProfileStoreLoadResult
    data class Invalid(val reason: String) : AppearanceProfileStoreLoadResult
    data class StorageFailed(val message: String? = null) : AppearanceProfileStoreLoadResult
}

sealed interface AppearanceProfileStoreSaveResult {
    data object Saved : AppearanceProfileStoreSaveResult
    data class StorageFailed(val message: String? = null) : AppearanceProfileStoreSaveResult
}

sealed interface AppearanceProfileWarning {
    data class CustomProfileStoreRejected(val reason: String) : AppearanceProfileWarning
}

data class AppearanceProfileState(
    val stockProfiles: List<AppearanceProfile>,
    val customProfiles: List<AppearanceProfile>,
    val activeLightProfileId: AppearanceProfileId?,
    val activeDarkProfileId: AppearanceProfileId?,
    val selectedLightDraft: AppearanceDraft?,
    val selectedDarkDraft: AppearanceDraft?,
    val warning: AppearanceProfileWarning? = null,
) {
    val profiles: List<AppearanceProfile> = stockProfiles + customProfiles

    fun selectedDraftFor(mode: AppearanceMode): AppearanceDraft? =
        when (mode) {
            AppearanceMode.LIGHT -> selectedLightDraft
            AppearanceMode.DARK -> selectedDarkDraft
        }
}

sealed interface AppearanceProfileLoadResult {
    data class Loaded(val state: AppearanceProfileState) : AppearanceProfileLoadResult
    data class StorageFailed(
        val state: AppearanceProfileState,
        val message: String? = null,
    ) : AppearanceProfileLoadResult
}

sealed interface AppearanceProfileSaveResult {
    data class Saved(
        val state: AppearanceProfileState,
        val profile: AppearanceProfile,
    ) : AppearanceProfileSaveResult

    data class Rejected(val reason: String) : AppearanceProfileSaveResult
    data class StorageFailed(val message: String? = null) : AppearanceProfileSaveResult
}

sealed interface AppearanceProfileSelectionResult {
    data class Selected(
        val state: AppearanceProfileState,
        val profile: AppearanceProfile,
    ) : AppearanceProfileSelectionResult

    data class ProfileMissing(val profileId: AppearanceProfileId) : AppearanceProfileSelectionResult
    data class StorageFailed(val message: String? = null) : AppearanceProfileSelectionResult
}

sealed interface AppearanceProfileResetResult {
    data class Reset(
        val state: AppearanceProfileState,
    ) : AppearanceProfileResetResult

    data class StorageFailed(val message: String? = null) : AppearanceProfileResetResult
}

sealed interface AppearanceProfileModeSwitchResult {
    data class Switched(
        val state: AppearanceProfileState,
    ) : AppearanceProfileModeSwitchResult

    data class StorageFailed(val message: String? = null) : AppearanceProfileModeSwitchResult
}
