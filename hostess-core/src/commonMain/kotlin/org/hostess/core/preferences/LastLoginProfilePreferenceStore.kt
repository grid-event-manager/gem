package org.hostess.core.preferences

import org.hostess.core.domain.AccountProfileId

interface LastLoginProfilePreferenceStore {
    fun load(): LastLoginProfilePreferenceLoadResult

    fun save(profileId: AccountProfileId): LastLoginProfilePreferenceSaveResult
}

sealed interface LastLoginProfilePreferenceLoadResult {
    data class Loaded(val profileId: AccountProfileId) : LastLoginProfilePreferenceLoadResult
    data object Missing : LastLoginProfilePreferenceLoadResult
    data class InvalidValue(val rawValue: String) : LastLoginProfilePreferenceLoadResult
    data class StorageFailed(val message: String? = null) : LastLoginProfilePreferenceLoadResult
}

sealed interface LastLoginProfilePreferenceSaveResult {
    data object Saved : LastLoginProfilePreferenceSaveResult
    data class StorageFailed(val message: String? = null) : LastLoginProfilePreferenceSaveResult
}
