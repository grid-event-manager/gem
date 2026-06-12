package org.gem.credential.vault

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalUserFileVaultKeySourceTest {
    @Test
    fun `creates loads and deletes desktop key file`() = withTempDirectory { dir ->
        val source = LocalUserFileVaultKeySource(dir, secureRandom = FillingSecureRandom(7))

        val loaded = assertIs<VaultKeySourceResult.Loaded>(source.getOrCreateKey())
        assertEquals(emptySet(), loaded.details)
        val keyFile = dir.resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)
        assertTrue(Files.exists(keyFile))
        assertEquals(JcaVaultKeyMaterial.RAW_AES_256_KEY_BYTES.toLong(), Files.size(keyFile))
        assertContentEquals(ByteArray(32) { 7 }, Files.readAllBytes(keyFile))
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            assertEquals(OWNER_ONLY_PERMISSIONS, Files.getPosixFilePermissions(keyFile))
        }

        assertIs<VaultKeySourceResult.Loaded>(source.getOrCreateKey())
        assertEquals(VaultKeySourceDeleteResult.Deleted, source.deleteKey())
        assertEquals(VaultKeySourceDeleteResult.Missing, source.deleteKey())
    }

    @Test
    fun `rejects wrong length key file`() = withTempDirectory { dir ->
        Files.createDirectories(dir)
        val keyFile = dir.resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)
        Files.write(keyFile, ByteArray(31), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        setOwnerOnlyIfPosix(keyFile)
        val source = LocalUserFileVaultKeySource(dir)

        val result = assertIs<VaultKeySourceResult.KeySourceFailed>(source.getOrCreateKey())

        assertEquals("invalid_key_file_length", result.message)
    }

    @Test
    fun `rejects group or world readable posix key file`() = withTempDirectory { dir ->
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return@withTempDirectory
        }
        Files.createDirectories(dir)
        val keyFile = dir.resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)
        Files.write(keyFile, ByteArray(32), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        Files.setPosixFilePermissions(
            keyFile,
            OWNER_ONLY_PERMISSIONS + PosixFilePermission.GROUP_READ,
        )
        val source = LocalUserFileVaultKeySource(dir)

        val result = assertIs<VaultKeySourceResult.KeySourceFailed>(source.getOrCreateKey())

        assertEquals("insecure_key_file_permissions", result.message)
    }

    @Test
    fun `reports windows non posix status detail while remaining usable`() = withTempDirectory { dir ->
        val source = nonPosixSource(dir, osName = "Windows 11")

        val loaded = assertIs<VaultKeySourceResult.Loaded>(source.getOrCreateKey())

        assertEquals(setOf("windows_user_profile_acl"), loaded.details)
    }

    @Test
    fun `reports non windows non posix status detail while remaining usable`() = withTempDirectory { dir ->
        val source = nonPosixSource(dir, osName = "Plan 9")

        val loaded = assertIs<VaultKeySourceResult.Loaded>(source.getOrCreateKey())

        assertEquals(setOf("posix_permissions_unavailable"), loaded.details)
    }

    private fun nonPosixSource(
        dir: Path,
        osName: String,
    ): LocalUserFileVaultKeySource =
        LocalUserFileVaultKeySource(
            keyDirectory = dir,
            osName = osName,
            secureRandom = FillingSecureRandom(3),
            permissionSupport = VaultFilePermissionSupport(posixSupported = false),
            overrideMarker = VaultFilePermissionSupportOverride,
        )

    private fun setOwnerOnlyIfPosix(path: Path) {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(path, OWNER_ONLY_PERMISSIONS)
        }
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("gem-vault-key-source-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private companion object {
        val OWNER_ONLY_PERMISSIONS: Set<PosixFilePermission> = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
        )
    }
}

internal class FillingSecureRandom(
    private val value: Byte,
) : SecureRandom() {
    constructor(value: Int) : this(value.toByte())

    override fun nextBytes(bytes: ByteArray) {
        bytes.fill(value)
    }
}
