package org.gem.credential.vault

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class GemVaultFileStore(
    vaultFile: Path,
) {
    private val vaultFile: Path = vaultFile.toAbsolutePath()

    fun read(): GemVaultFileReadResult =
        try {
            if (!Files.exists(vaultFile)) {
                GemVaultFileReadResult.Missing
            } else {
                GemVaultFileReadResult.Read(Files.readAllBytes(vaultFile))
            }
        } catch (_: IOException) {
            GemVaultFileReadResult.StorageFailed("storage_failed")
        } catch (_: SecurityException) {
            GemVaultFileReadResult.StorageFailed("storage_failed")
        }

    fun writeAtomic(bytes: ByteArray): GemVaultFileWriteResult {
        val parent = vaultFile.parent
        var tempFile: Path? = null
        val details = mutableSetOf<String>()
        return try {
            if (parent != null) {
                Files.createDirectories(parent)
                tempFile = Files.createTempFile(parent, vaultFile.tempPrefix(), ".tmp")
            } else {
                tempFile = Files.createTempFile(vaultFile.tempPrefix(), ".tmp")
            }
            FileChannel.open(
                tempFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            ).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
                channel.force(true)
            }
            moveTempFile(tempFile, details)
            fsyncParent(parent, details)
            GemVaultFileWriteResult.Written(details.toSet())
        } catch (_: IOException) {
            tempFile?.deleteIfPresent()
            GemVaultFileWriteResult.StorageFailed("storage_failed")
        } catch (_: SecurityException) {
            tempFile?.deleteIfPresent()
            GemVaultFileWriteResult.StorageFailed("storage_failed")
        }
    }

    fun delete(): GemVaultFileDeleteResult =
        try {
            if (Files.deleteIfExists(vaultFile)) {
                GemVaultFileDeleteResult.Deleted
            } else {
                GemVaultFileDeleteResult.Missing
            }
        } catch (_: IOException) {
            GemVaultFileDeleteResult.StorageFailed("storage_failed")
        } catch (_: SecurityException) {
            GemVaultFileDeleteResult.StorageFailed("storage_failed")
        }

    private fun moveTempFile(
        tempFile: Path,
        details: MutableSet<String>,
    ) {
        try {
            Files.move(tempFile, vaultFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            details += "atomic_move_unavailable"
            Files.move(tempFile, vaultFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun fsyncParent(
        parent: Path?,
        details: MutableSet<String>,
    ) {
        if (parent == null) {
            return
        }
        try {
            FileChannel.open(parent, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            details += "directory_fsync_unavailable"
        } catch (_: AccessDeniedException) {
            details += "directory_fsync_unavailable"
        } catch (_: FileSystemException) {
            details += "directory_fsync_unavailable"
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
        const val MIN_TEMP_PREFIX_LENGTH: Int = 3
    }
}

sealed interface GemVaultFileReadResult {
    data object Missing : GemVaultFileReadResult
    data class Read(val bytes: ByteArray) : GemVaultFileReadResult
    data class StorageFailed(val message: String? = null) : GemVaultFileReadResult
}

sealed interface GemVaultFileWriteResult {
    data class Written(val details: Set<String> = emptySet()) : GemVaultFileWriteResult
    data class StorageFailed(val message: String? = null) : GemVaultFileWriteResult
}

sealed interface GemVaultFileDeleteResult {
    data object Deleted : GemVaultFileDeleteResult
    data object Missing : GemVaultFileDeleteResult
    data class StorageFailed(val message: String? = null) : GemVaultFileDeleteResult
}
