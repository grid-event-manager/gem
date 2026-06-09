package org.hostess.credential.vault

import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom

class LocalUserFileVaultKeySource private constructor(
    private val keyDirectory: Path,
    private val osName: String,
    private val secureRandom: SecureRandom,
    private val permissionSupport: VaultFilePermissionSupport,
) : VaultKeySource {
    constructor(
        keyDirectory: Path,
        osName: String = System.getProperty("os.name").orEmpty(),
        secureRandom: SecureRandom = SecureRandom(),
    ) : this(
        keyDirectory = keyDirectory,
        osName = osName,
        secureRandom = secureRandom,
        permissionSupport = VaultFilePermissionSupport.detect(),
    )

    internal constructor(
        keyDirectory: Path,
        osName: String,
        secureRandom: SecureRandom,
        permissionSupport: VaultFilePermissionSupport,
        @Suppress("UNUSED_PARAMETER") overrideMarker: VaultFilePermissionSupportOverride,
    ) : this(
        keyDirectory = keyDirectory,
        osName = osName,
        secureRandom = secureRandom,
        permissionSupport = permissionSupport,
    )

    private val keyFile: Path = keyDirectory.resolve(KEY_FILE_NAME)

    override fun getOrCreateKey(): VaultKeySourceResult =
        try {
            Files.createDirectories(keyDirectory)
            if (Files.exists(keyFile)) {
                loadExistingKey()
            } else {
                createKeyFile()
            }
        } catch (_: IOException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: SecurityException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: UnsupportedOperationException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        }

    override fun deleteKey(): VaultKeySourceDeleteResult =
        try {
            if (Files.deleteIfExists(keyFile)) {
                VaultKeySourceDeleteResult.Deleted
            } else {
                VaultKeySourceDeleteResult.Missing
            }
        } catch (_: IOException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        } catch (_: SecurityException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        }

    private fun createKeyFile(): VaultKeySourceResult {
        val rawKey = ByteArray(JcaVaultKeyMaterial.RAW_AES_256_KEY_BYTES)
        secureRandom.nextBytes(rawKey)
        return try {
            if (permissionSupport.posixSupported) {
                Files.createFile(keyFile, PosixFilePermissions.asFileAttribute(OWNER_ONLY_PERMISSIONS))
                Files.write(keyFile, rawKey, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                Files.setPosixFilePermissions(keyFile, OWNER_ONLY_PERMISSIONS)
                if (!hasExactOwnerOnlyPermissions()) {
                    return VaultKeySourceResult.KeySourceFailed("insecure_key_file_permissions")
                }
            } else {
                Files.write(keyFile, rawKey, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            }
            materialFrom(rawKey)
        } catch (_: FileAlreadyExistsException) {
            loadExistingKey()
        } catch (_: IOException) {
            keyFile.deletePartialKeyFile()
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: SecurityException) {
            keyFile.deletePartialKeyFile()
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: UnsupportedOperationException) {
            keyFile.deletePartialKeyFile()
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } finally {
            rawKey.fill(0)
        }
    }

    private fun loadExistingKey(): VaultKeySourceResult {
        if (permissionSupport.posixSupported && !hasExactOwnerOnlyPermissions()) {
            return VaultKeySourceResult.KeySourceFailed("insecure_key_file_permissions")
        }
        val rawKey = Files.readAllBytes(keyFile)
        return try {
            materialFrom(rawKey)
        } finally {
            rawKey.fill(0)
        }
    }

    private fun materialFrom(rawKey: ByteArray): VaultKeySourceResult {
        val material = JcaVaultKeyMaterial.fromRawKeyBytes(rawKey)
            ?: return VaultKeySourceResult.KeySourceFailed("invalid_key_file_length")
        return VaultKeySourceResult.Loaded(material, statusDetails())
    }

    private fun hasExactOwnerOnlyPermissions(): Boolean =
        Files.getPosixFilePermissions(keyFile) == OWNER_ONLY_PERMISSIONS

    private fun statusDetails(): Set<String> =
        when {
            permissionSupport.posixSupported -> emptySet()
            osName.startsWith("Windows", ignoreCase = true) -> setOf("windows_user_profile_acl")
            else -> setOf("posix_permissions_unavailable")
        }

    private fun Path.deletePartialKeyFile() {
        try {
            Files.deleteIfExists(this)
        } catch (_: IOException) {
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val KEY_FILE_NAME: String = "key.bin"

        private val OWNER_ONLY_PERMISSIONS: Set<PosixFilePermission> = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
        )
    }
}

internal data class VaultFilePermissionSupport(
    val posixSupported: Boolean,
) {
    companion object {
        fun detect(): VaultFilePermissionSupport =
            VaultFilePermissionSupport(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
    }
}

internal object VaultFilePermissionSupportOverride
