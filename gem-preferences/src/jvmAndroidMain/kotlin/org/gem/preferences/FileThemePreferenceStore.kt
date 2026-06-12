package org.gem.preferences

import java.nio.file.Path
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.core.theme.ThemePreferenceStore

class FileThemePreferenceStore internal constructor(
    preferenceFile: Path,
    private val codec: ThemePreferenceFileCodec,
    moveStrategy: ThemePreferenceFileMoveStrategy,
) : ThemePreferenceStore {
    private val storage = PreferenceFileStorage(preferenceFile, moveStrategy)

    constructor(
        preferenceFile: Path,
        codec: ThemePreferenceFileCodec = ThemePreferenceFileCodec(),
    ) : this(preferenceFile, codec, NioPreferenceFileMoveStrategy)

    override fun load(): ThemePreferenceLoadResult =
        when (val read = storage.readUtf8()) {
            is PreferenceFileReadResult.Read -> codec.decode(read.value)
            PreferenceFileReadResult.Missing -> ThemePreferenceLoadResult.Missing
            is PreferenceFileReadResult.StorageFailed -> ThemePreferenceLoadResult.StorageFailed(read.message)
        }

    override fun save(preference: ThemePreference): ThemePreferenceSaveResult =
        when (val written = storage.writeUtf8(codec.encode(preference))) {
            PreferenceFileWriteResult.Written -> ThemePreferenceSaveResult.Saved
            is PreferenceFileWriteResult.StorageFailed -> ThemePreferenceSaveResult.StorageFailed(written.message)
        }
}

internal typealias ThemePreferenceFileMoveStrategy = PreferenceFileMoveStrategy
