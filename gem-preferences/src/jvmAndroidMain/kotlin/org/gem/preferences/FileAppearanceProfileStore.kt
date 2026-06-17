package org.gem.preferences

import java.nio.file.Path
import org.gem.core.appearance.AppearanceProfileStore
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSaveResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot

class FileAppearanceProfileStore internal constructor(
    preferenceFile: Path,
    private val codec: AppearanceProfileFileCodec,
    moveStrategy: AppearanceProfileFileMoveStrategy,
) : AppearanceProfileStore {
    private val storage = PreferenceFileStorage(preferenceFile, moveStrategy)

    constructor(
        preferenceFile: Path,
        codec: AppearanceProfileFileCodec = AppearanceProfileFileCodec(),
    ) : this(preferenceFile, codec, NioPreferenceFileMoveStrategy)

    override fun load(): AppearanceProfileStoreLoadResult =
        when (val read = storage.readUtf8()) {
            is PreferenceFileReadResult.Read -> codec.decode(read.value)
            PreferenceFileReadResult.Missing -> AppearanceProfileStoreLoadResult.Missing
            is PreferenceFileReadResult.StorageFailed -> {
                AppearanceProfileStoreLoadResult.StorageFailed(read.message)
            }
        }

    override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult =
        when (val written = storage.writeUtf8(codec.encode(snapshot))) {
            PreferenceFileWriteResult.Written -> AppearanceProfileStoreSaveResult.Saved
            is PreferenceFileWriteResult.StorageFailed -> {
                AppearanceProfileStoreSaveResult.StorageFailed(written.message)
            }
        }
}

internal typealias AppearanceProfileFileMoveStrategy = PreferenceFileMoveStrategy
