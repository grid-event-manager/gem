package org.hostess.core.ports

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile

interface AccountProfileStore {
    fun list(): AccountProfileStoreListResult

    fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult

    fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult

    fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult
}

sealed interface AccountProfileStoreListResult {
    data class Listed(val profiles: List<SavedAccountProfile>) : AccountProfileStoreListResult
    data class CorruptVault(val message: String? = null) : AccountProfileStoreListResult
    data class StorageFailed(val message: String? = null) : AccountProfileStoreListResult
}

sealed interface AccountProfileStoreSaveResult {
    data class Saved(val profile: SavedAccountProfile) : AccountProfileStoreSaveResult
    data class CorruptVault(val message: String? = null) : AccountProfileStoreSaveResult
    data class StorageFailed(val message: String? = null) : AccountProfileStoreSaveResult
}

sealed interface AccountProfileStoreUpdateResult {
    data class Updated(val profile: SavedAccountProfile) : AccountProfileStoreUpdateResult
    data class Missing(val profileId: AccountProfileId, val message: String? = null) : AccountProfileStoreUpdateResult
    data class CorruptVault(val message: String? = null) : AccountProfileStoreUpdateResult
    data class StorageFailed(val message: String? = null) : AccountProfileStoreUpdateResult
}

sealed interface AccountProfileStoreDeleteResult {
    data class Deleted(val profileId: AccountProfileId) : AccountProfileStoreDeleteResult
    data class Missing(val profileId: AccountProfileId, val message: String? = null) : AccountProfileStoreDeleteResult
    data class CorruptVault(val message: String? = null) : AccountProfileStoreDeleteResult
    data class StorageFailed(val message: String? = null) : AccountProfileStoreDeleteResult
}
