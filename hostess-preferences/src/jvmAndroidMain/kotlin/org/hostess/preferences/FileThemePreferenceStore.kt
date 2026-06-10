package org.hostess.preferences

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.core.theme.ThemePreferenceStore

class FileThemePreferenceStore internal constructor(
    private val preferenceFile: Path,
    private val codec: ThemePreferenceFileCodec,
    private val moveStrategy: ThemePreferenceFileMoveStrategy,
) : ThemePreferenceStore {
    constructor(
        preferenceFile: Path,
        codec: ThemePreferenceFileCodec = ThemePreferenceFileCodec(),
    ) : this(preferenceFile, codec, NioThemePreferenceFileMoveStrategy)

    override fun load(): ThemePreferenceLoadResult =
        try {
            if (!Files.exists(preferenceFile)) {
                ThemePreferenceLoadResult.Missing
            } else {
                codec.decode(String(Files.readAllBytes(preferenceFile), StandardCharsets.UTF_8))
            }
        } catch (_: IOException) {
            ThemePreferenceLoadResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        } catch (_: SecurityException) {
            ThemePreferenceLoadResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        }

    override fun save(preference: ThemePreference): ThemePreferenceSaveResult {
        val parent = preferenceFile.parent
        var tempFile: Path? = null
        return try {
            if (parent != null) {
                Files.createDirectories(parent)
                tempFile = Files.createTempFile(parent, preferenceFile.tempPrefix(), TEMP_SUFFIX)
            } else {
                tempFile = Files.createTempFile(preferenceFile.tempPrefix(), TEMP_SUFFIX)
            }
            Files.write(
                tempFile,
                codec.encode(preference).toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            moveTempFile(tempFile)
            fsyncParent(parent)
            ThemePreferenceSaveResult.Saved
        } catch (_: IOException) {
            tempFile?.deleteIfPresent()
            ThemePreferenceSaveResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        } catch (_: SecurityException) {
            tempFile?.deleteIfPresent()
            ThemePreferenceSaveResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        }
    }

    private fun moveTempFile(tempFile: Path) {
        try {
            moveStrategy.moveAtomically(tempFile, preferenceFile)
        } catch (_: AtomicMoveNotSupportedException) {
            moveStrategy.moveReplace(tempFile, preferenceFile)
        }
    }

    private fun fsyncParent(parent: Path?) {
        if (parent == null) {
            return
        }
        try {
            FileChannel.open(parent, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            // Directory fsync is advisory here; the preference file has already been atomically moved.
        } catch (_: AccessDeniedException) {
            // Directory fsync is advisory here; the preference file has already been atomically moved.
        } catch (_: FileSystemException) {
            // Directory fsync is advisory here; the preference file has already been atomically moved.
        }
    }

    private fun Path.deleteIfPresent() {
        try {
            Files.deleteIfExists(this)
        } catch (_: IOException) {
            // Best-effort cleanup only; the caller already returns the storage failure.
        } catch (_: SecurityException) {
            // Best-effort cleanup only; the caller already returns the storage failure.
        }
    }

    private fun Path.tempPrefix(): String =
        fileName.toString().padEnd(MIN_TEMP_PREFIX_LENGTH, '_')

    private companion object {
        const val STORAGE_FAILED_MESSAGE: String = "storage_failed"
        const val TEMP_SUFFIX: String = ".tmp"
        const val MIN_TEMP_PREFIX_LENGTH: Int = 3
    }
}

internal interface ThemePreferenceFileMoveStrategy {
    fun moveAtomically(
        source: Path,
        target: Path,
    )

    fun moveReplace(
        source: Path,
        target: Path,
    )
}

private object NioThemePreferenceFileMoveStrategy : ThemePreferenceFileMoveStrategy {
    override fun moveAtomically(
        source: Path,
        target: Path,
    ) {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun moveReplace(
        source: Path,
        target: Path,
    ) {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
