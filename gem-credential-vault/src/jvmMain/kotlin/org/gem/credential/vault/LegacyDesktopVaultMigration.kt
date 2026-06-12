package org.gem.credential.vault

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object LegacyDesktopVaultMigration {
    fun run(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): LegacyDesktopVaultMigrationResult {
        val legacyPath = Path.of(legacyVaultDirectory(osName, env, userHome))
        val canonicalPath = Path.of(DesktopVaultPaths.defaultVaultDirectory(osName, env, userHome))
        return migratePath(legacyPath, canonicalPath)
    }

    internal fun migratePath(
        legacyPath: Path,
        canonicalPath: Path,
        atomicMove: (Path, Path) -> Path = { source, target ->
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        },
        fallbackMove: (Path, Path) -> Path = { source, target ->
            Files.move(source, target)
        },
    ): LegacyDesktopVaultMigrationResult {
        if (!Files.exists(legacyPath)) {
            return LegacyDesktopVaultMigrationResult.NoLegacyData
        }
        if (Files.exists(canonicalPath)) {
            return LegacyDesktopVaultMigrationResult.CanonicalAlreadyPresent(
                legacyPath = legacyPath,
                canonicalPath = canonicalPath,
            )
        }

        try {
            canonicalPath.parent?.let(Files::createDirectories)
            moveLegacyPath(legacyPath, canonicalPath, atomicMove, fallbackMove)
        } catch (failure: IOException) {
            throw LegacyDesktopVaultMigrationException(failure)
        } catch (failure: SecurityException) {
            throw LegacyDesktopVaultMigrationException(failure)
        }
        return LegacyDesktopVaultMigrationResult.Moved(
            legacyPath = legacyPath,
            canonicalPath = canonicalPath,
        )
    }

    private fun moveLegacyPath(
        legacyPath: Path,
        canonicalPath: Path,
        atomicMove: (Path, Path) -> Path,
        fallbackMove: (Path, Path) -> Path,
    ) {
        try {
            atomicMove(legacyPath, canonicalPath)
        } catch (_: AtomicMoveNotSupportedException) {
            fallbackMove(legacyPath, canonicalPath)
        }
    }

    private fun legacyVaultDirectory(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String =
        joinPath(desktopDataBase(osName, env, userHome), "Hostess", "vault")

    private fun desktopDataBase(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String {
        val normalizedOs = osName.lowercase()
        return when {
            normalizedOs.startsWith("windows") ->
                env["APPDATA"].orEmpty().ifBlank { joinPath(userHome, "AppData", "Roaming") }
            normalizedOs.startsWith("mac") || normalizedOs.contains("darwin") ->
                joinPath(userHome, "Library", "Application Support")
            else ->
                env["XDG_DATA_HOME"].orEmpty().ifBlank { joinPath(userHome, ".local", "share") }
        }
    }

    private fun joinPath(
        first: String,
        vararg more: String,
    ): String {
        val separator = if ('\\' in first) "\\" else "/"
        return (listOf(first.trimEnd('/', '\\')) + more.map { it.trim('/', '\\') })
            .joinToString(separator)
    }
}

sealed class LegacyDesktopVaultMigrationResult {
    data object NoLegacyData : LegacyDesktopVaultMigrationResult()

    data class CanonicalAlreadyPresent(
        val legacyPath: Path,
        val canonicalPath: Path,
    ) : LegacyDesktopVaultMigrationResult()

    data class Moved(
        val legacyPath: Path,
        val canonicalPath: Path,
    ) : LegacyDesktopVaultMigrationResult()
}

class LegacyDesktopVaultMigrationException(
    cause: Throwable,
) : IllegalStateException("Unable to move legacy desktop vault data into gem storage.", cause)
