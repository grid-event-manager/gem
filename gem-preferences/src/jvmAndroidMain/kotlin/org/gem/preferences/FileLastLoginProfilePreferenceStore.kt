package org.gem.preferences

import java.nio.file.Path
import org.gem.core.domain.AccountProfileId
import org.gem.core.preferences.LastLoginProfilePreferenceLoadResult
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import org.gem.core.preferences.LastLoginProfilePreferenceStore

class FileLastLoginProfilePreferenceStore internal constructor(
    preferenceFile: Path,
    moveStrategy: PreferenceFileMoveStrategy,
) : LastLoginProfilePreferenceStore {
    private val storage = PreferenceFileStorage(preferenceFile, moveStrategy)

    constructor(preferenceFile: Path) : this(preferenceFile, NioPreferenceFileMoveStrategy)

    override fun load(): LastLoginProfilePreferenceLoadResult =
        when (val read = storage.readUtf8()) {
            is PreferenceFileReadResult.Read -> decode(read.value)
            PreferenceFileReadResult.Missing -> LastLoginProfilePreferenceLoadResult.Missing
            is PreferenceFileReadResult.StorageFailed -> {
                LastLoginProfilePreferenceLoadResult.StorageFailed(read.message)
            }
        }

    override fun save(profileId: AccountProfileId): LastLoginProfilePreferenceSaveResult =
        when (val written = storage.writeUtf8(profileId.value)) {
            PreferenceFileWriteResult.Written -> LastLoginProfilePreferenceSaveResult.Saved
            is PreferenceFileWriteResult.StorageFailed -> {
                LastLoginProfilePreferenceSaveResult.StorageFailed(written.message)
            }
        }

    private fun decode(rawValue: String): LastLoginProfilePreferenceLoadResult {
        val trimmed = rawValue.trim()
        return try {
            LastLoginProfilePreferenceLoadResult.Loaded(AccountProfileId(trimmed))
        } catch (_: IllegalArgumentException) {
            LastLoginProfilePreferenceLoadResult.InvalidValue(trimmed)
        }
    }
}
