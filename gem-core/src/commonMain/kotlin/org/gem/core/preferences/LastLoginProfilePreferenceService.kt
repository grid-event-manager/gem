package org.gem.core.preferences

import org.gem.core.domain.AccountProfileId

class LastLoginProfilePreferenceService(
    private val store: LastLoginProfilePreferenceStore,
) {
    fun loadPreference(): LastLoginProfilePreferenceSnapshot =
        when (val loaded = store.load()) {
            is LastLoginProfilePreferenceLoadResult.Loaded -> {
                LastLoginProfilePreferenceSnapshot(profileId = loaded.profileId)
            }
            LastLoginProfilePreferenceLoadResult.Missing -> LastLoginProfilePreferenceSnapshot(profileId = null)
            is LastLoginProfilePreferenceLoadResult.InvalidValue -> LastLoginProfilePreferenceSnapshot(
                profileId = null,
                warning = LastLoginProfilePreferenceWarning.InvalidValue(loaded.rawValue),
            )
            is LastLoginProfilePreferenceLoadResult.StorageFailed -> LastLoginProfilePreferenceSnapshot(
                profileId = null,
                warning = LastLoginProfilePreferenceWarning.StorageFailed(loaded.message),
            )
        }

    fun saveProfileId(profileId: AccountProfileId): LastLoginProfilePreferenceSaveResult =
        store.save(profileId)
}

data class LastLoginProfilePreferenceSnapshot(
    val profileId: AccountProfileId?,
    val warning: LastLoginProfilePreferenceWarning? = null,
)

sealed interface LastLoginProfilePreferenceWarning {
    data class InvalidValue(val rawValue: String) : LastLoginProfilePreferenceWarning
    data class StorageFailed(val message: String? = null) : LastLoginProfilePreferenceWarning
}
