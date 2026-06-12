package org.gem.credential.vault

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class LegacyDesktopVaultMigrationTest {
    @Test
    fun `moves legacy Hostess vault directory to gem vault directory`() {
        withTempDataHome { dataHome ->
            val legacyVault = dataHome.resolve("Hostess/vault")
            val canonicalVault = dataHome.resolve("gem/vault")
            Files.createDirectories(legacyVault)
            Files.write(legacyVault.resolve("vault.bin"), byteArrayOf(1, 2, 3))
            Files.write(legacyVault.resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME), byteArrayOf(4, 5, 6))

            val result = LegacyDesktopVaultMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopVaultMigrationResult.Moved>(result)
            assertFalse(Files.exists(legacyVault))
            assertContentEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(canonicalVault.resolve("vault.bin")))
            assertContentEquals(
                byteArrayOf(4, 5, 6),
                Files.readAllBytes(canonicalVault.resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)),
            )
        }
    }

    @Test
    fun `keeps canonical gem vault authoritative when legacy and canonical vaults both exist`() {
        withTempDataHome { dataHome ->
            val legacyVault = dataHome.resolve("Hostess/vault")
            val canonicalVault = dataHome.resolve("gem/vault")
            Files.createDirectories(legacyVault)
            Files.createDirectories(canonicalVault)
            Files.write(legacyVault.resolve("vault.bin"), byteArrayOf(1))
            Files.write(canonicalVault.resolve("vault.bin"), byteArrayOf(9))

            val result = LegacyDesktopVaultMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopVaultMigrationResult.CanonicalAlreadyPresent>(result)
            assertContentEquals(byteArrayOf(1), Files.readAllBytes(legacyVault.resolve("vault.bin")))
            assertContentEquals(byteArrayOf(9), Files.readAllBytes(canonicalVault.resolve("vault.bin")))
        }
    }

    @Test
    fun `returns no legacy data when old vault path is absent`() {
        withTempDataHome { dataHome ->
            val result = LegacyDesktopVaultMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopVaultMigrationResult.NoLegacyData>(result)
            assertFalse(Files.exists(dataHome.resolve("gem/vault")))
        }
    }

    @Test
    fun `falls back to non atomic vault move when atomic move is unsupported`() {
        withTempDataHome { dataHome ->
            val legacyVault = dataHome.resolve("Hostess/vault")
            val canonicalVault = dataHome.resolve("gem/vault")
            Files.createDirectories(legacyVault)
            Files.write(legacyVault.resolve("vault.bin"), byteArrayOf(7))

            val result = LegacyDesktopVaultMigration.migratePath(
                legacyPath = legacyVault,
                canonicalPath = canonicalVault,
                atomicMove = { _, _ ->
                    throw AtomicMoveNotSupportedException("legacy", "canonical", "unsupported")
                },
                fallbackMove = { source, target ->
                    Files.move(source, target)
                },
            )

            assertIs<LegacyDesktopVaultMigrationResult.Moved>(result)
            assertFalse(Files.exists(legacyVault))
            assertContentEquals(byteArrayOf(7), Files.readAllBytes(canonicalVault.resolve("vault.bin")))
        }
    }

    @Test
    fun `fails closed when vault move fails`() {
        withTempDataHome { dataHome ->
            val legacyVault = dataHome.resolve("Hostess/vault")
            val canonicalVault = dataHome.resolve("gem/vault")
            Files.createDirectories(legacyVault)
            Files.write(legacyVault.resolve("vault.bin"), byteArrayOf(8))

            assertFailsWith<LegacyDesktopVaultMigrationException> {
                LegacyDesktopVaultMigration.migratePath(
                    legacyPath = legacyVault,
                    canonicalPath = canonicalVault,
                    atomicMove = { _, _ -> throw IOException("denied") },
                )
            }

            assertTrue(Files.exists(legacyVault.resolve("vault.bin")))
            assertFalse(Files.exists(canonicalVault))
        }
    }

    private fun withTempDataHome(block: (Path) -> Unit) {
        val dataHome = Files.createTempDirectory("gem-vault-migration-test")
        try {
            block(dataHome)
        } finally {
            dataHome.deleteRecursively()
        }
    }

    private fun Path.deleteRecursively() {
        if (!Files.exists(this)) {
            return
        }
        Files.walk(this).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}
