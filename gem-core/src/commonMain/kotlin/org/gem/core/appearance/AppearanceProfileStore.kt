package org.gem.core.appearance

data class AppearanceProfileStoreSnapshot(
    val customProfiles: List<AppearanceProfile>,
    val activeLightProfileId: AppearanceProfileId?,
    val activeDarkProfileId: AppearanceProfileId?,
) {
    init {
        require(customProfiles.all { it.source == AppearanceProfileSource.CUSTOM }) {
            "AppearanceProfileStoreSnapshot accepts custom profiles only."
        }
    }
}

interface AppearanceProfileStore {
    fun load(): AppearanceProfileStoreLoadResult

    fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult
}
