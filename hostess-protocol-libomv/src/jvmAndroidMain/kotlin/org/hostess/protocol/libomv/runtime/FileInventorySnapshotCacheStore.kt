package org.hostess.protocol.libomv.runtime

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

internal class FileInventorySnapshotCacheStore(
    private val cacheDirectory: Path,
) : InventorySnapshotCacheStore {
    override fun load(key: InventorySnapshotCacheKey): InventorySnapshot? {
        val cacheFile = cacheFile(key)
        return try {
            if (!Files.exists(cacheFile)) {
                null
            } else {
                InventorySnapshotCacheCodec.decode(
                    String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8),
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override fun save(
        key: InventorySnapshotCacheKey,
        snapshot: InventorySnapshot,
    ) {
        var tempFile: Path? = null
        try {
            Files.createDirectories(cacheDirectory)
            tempFile = Files.createTempFile(cacheDirectory, CACHE_TEMP_PREFIX, CACHE_TEMP_SUFFIX)
            Files.write(
                tempFile,
                InventorySnapshotCacheCodec.encode(snapshot).toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            moveTempFile(tempFile, cacheFile(key))
            fsyncParent(cacheDirectory)
        } catch (_: IOException) {
            tempFile?.deleteIfPresent()
        } catch (_: SecurityException) {
            tempFile?.deleteIfPresent()
        }
    }

    private fun cacheFile(key: InventorySnapshotCacheKey): Path =
        cacheDirectory.resolve("${CACHE_FILE_PREFIX}${fileSafe(key.agentId)}-${fileSafe(key.rootId)}$CACHE_FILE_SUFFIX")

    private fun moveTempFile(
        tempFile: Path,
        cacheFile: Path,
    ) {
        try {
            Files.move(tempFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun fsyncParent(parent: Path) {
        try {
            FileChannel.open(parent, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            // Cache persistence is best-effort; the snapshot can be rebuilt from the live inventory source.
        } catch (_: AccessDeniedException) {
            // Cache persistence is best-effort; the snapshot can be rebuilt from the live inventory source.
        } catch (_: FileSystemException) {
            // Cache persistence is best-effort; the snapshot can be rebuilt from the live inventory source.
        }
    }

    private fun Path.deleteIfPresent() {
        try {
            Files.deleteIfExists(this)
        } catch (_: IOException) {
            // Best-effort cleanup only.
        } catch (_: SecurityException) {
            // Best-effort cleanup only.
        }
    }

    private fun fileSafe(value: String): String = buildString {
        value.forEach { char ->
            append(
                when {
                    char in 'a'..'z' -> char
                    char in 'A'..'Z' -> char
                    char in '0'..'9' -> char
                    char == '-' -> char
                    else -> '_'
                },
            )
        }
    }.ifBlank { "blank" }

    private companion object {
        const val CACHE_FILE_PREFIX = "inventory-"
        const val CACHE_FILE_SUFFIX = ".tsv"
        const val CACHE_TEMP_PREFIX = "inventory-cache"
        const val CACHE_TEMP_SUFFIX = ".tmp"
    }
}
