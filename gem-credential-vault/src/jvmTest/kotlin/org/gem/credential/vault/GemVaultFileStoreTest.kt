package org.gem.credential.vault

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GemVaultFileStoreTest {
    @Test
    fun `reads missing file then writes reads and deletes vault bytes`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("vault.bin")
        val store = GemVaultFileStore(vaultPath)
        val bytes = "encrypted synthetic bytes".encodeToByteArray()

        assertEquals(GemVaultFileReadResult.Missing, store.read())

        assertIs<GemVaultFileWriteResult.Written>(store.writeAtomic(bytes))
        val read = assertIs<GemVaultFileReadResult.Read>(store.read())
        assertContentEquals(bytes, read.bytes)

        assertEquals(GemVaultFileDeleteResult.Deleted, store.delete())
        assertEquals(GemVaultFileDeleteResult.Missing, store.delete())
        assertEquals(GemVaultFileReadResult.Missing, store.read())
    }

    @Test
    fun `write creates parent directories`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("nested").resolve("vault.bin")
        val store = GemVaultFileStore(vaultPath)

        assertIs<GemVaultFileWriteResult.Written>(store.writeAtomic(byteArrayOf(1, 2, 3)))

        assertTrue(Files.exists(vaultPath))
    }

    @Test
    fun `write handles short vault filenames`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("v")
        val store = GemVaultFileStore(vaultPath)

        assertIs<GemVaultFileWriteResult.Written>(store.writeAtomic(byteArrayOf(1, 2, 3)))
        assertTrue(Files.exists(vaultPath))
    }

    @Test
    fun `write failure removes sibling temp file`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("vault.bin")
        Files.createDirectory(vaultPath)
        val store = GemVaultFileStore(vaultPath)

        assertIs<GemVaultFileWriteResult.StorageFailed>(store.writeAtomic(byteArrayOf(1, 2, 3)))

        val leakedTemps = dir.listDirectoryEntries().filter { it.fileName.toString().contains(".tmp") }
        assertTrue(leakedTemps.isEmpty(), leakedTemps.joinToString())
    }

    @Test
    fun `storage failure is redacted for invalid parent path`() = withTempDirectory { dir ->
        val parentFile = dir.resolve("parent-file")
        Files.write(parentFile, byteArrayOf(1))
        val store = GemVaultFileStore(parentFile.resolve("vault.bin"))

        val result = assertIs<GemVaultFileWriteResult.StorageFailed>(store.writeAtomic(byteArrayOf(1, 2, 3)))

        assertEquals("storage_failed", result.message)
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("gem-vault-store-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
