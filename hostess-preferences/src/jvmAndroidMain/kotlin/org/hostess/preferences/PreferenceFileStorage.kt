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

internal class PreferenceFileStorage(
    private val preferenceFile: Path,
    private val moveStrategy: PreferenceFileMoveStrategy = NioPreferenceFileMoveStrategy,
) {
    fun readUtf8(): PreferenceFileReadResult =
        try {
            if (!Files.exists(preferenceFile)) {
                PreferenceFileReadResult.Missing
            } else {
                PreferenceFileReadResult.Read(String(Files.readAllBytes(preferenceFile), StandardCharsets.UTF_8))
            }
        } catch (_: IOException) {
            PreferenceFileReadResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        } catch (_: SecurityException) {
            PreferenceFileReadResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        }

    fun writeUtf8(value: String): PreferenceFileWriteResult {
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
                value.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            moveTempFile(tempFile)
            fsyncParent(parent)
            PreferenceFileWriteResult.Written
        } catch (_: IOException) {
            tempFile?.deleteIfPresent()
            PreferenceFileWriteResult.StorageFailed(STORAGE_FAILED_MESSAGE)
        } catch (_: SecurityException) {
            tempFile?.deleteIfPresent()
            PreferenceFileWriteResult.StorageFailed(STORAGE_FAILED_MESSAGE)
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

internal sealed interface PreferenceFileReadResult {
    data class Read(val value: String) : PreferenceFileReadResult
    data object Missing : PreferenceFileReadResult
    data class StorageFailed(val message: String) : PreferenceFileReadResult
}

internal sealed interface PreferenceFileWriteResult {
    data object Written : PreferenceFileWriteResult
    data class StorageFailed(val message: String) : PreferenceFileWriteResult
}

internal interface PreferenceFileMoveStrategy {
    fun moveAtomically(
        source: Path,
        target: Path,
    )

    fun moveReplace(
        source: Path,
        target: Path,
    )
}

internal object NioPreferenceFileMoveStrategy : PreferenceFileMoveStrategy {
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
