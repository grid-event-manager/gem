package org.gem.preferences

import java.nio.file.Path
import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult
import org.gem.core.language.LanguagePreferenceSaveResult
import org.gem.core.language.LanguagePreferenceStore

class FileLanguagePreferenceStore internal constructor(
    preferenceFile: Path,
    private val codec: LanguagePreferenceFileCodec,
    moveStrategy: LanguagePreferenceFileMoveStrategy,
) : LanguagePreferenceStore {
    private val storage = PreferenceFileStorage(preferenceFile, moveStrategy)

    constructor(
        preferenceFile: Path,
        codec: LanguagePreferenceFileCodec = LanguagePreferenceFileCodec(),
    ) : this(preferenceFile, codec, NioPreferenceFileMoveStrategy)

    override fun load(): LanguagePreferenceLoadResult =
        when (val read = storage.readUtf8()) {
            is PreferenceFileReadResult.Read -> codec.decode(read.value)
            PreferenceFileReadResult.Missing -> LanguagePreferenceLoadResult.Missing
            is PreferenceFileReadResult.StorageFailed -> LanguagePreferenceLoadResult.StorageFailed(read.message)
        }

    override fun save(preference: LanguagePreference): LanguagePreferenceSaveResult =
        when (val written = storage.writeUtf8(codec.encode(preference))) {
            PreferenceFileWriteResult.Written -> LanguagePreferenceSaveResult.Saved
            is PreferenceFileWriteResult.StorageFailed -> LanguagePreferenceSaveResult.StorageFailed(written.message)
        }
}

internal typealias LanguagePreferenceFileMoveStrategy = PreferenceFileMoveStrategy
