package org.hostess.preferences

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import kotlin.io.path.listDirectoryEntries
import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import org.hostess.core.theme.ThemePreferenceSaveResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FileThemePreferenceStoreTest {
    @Test
    fun `loads missing file then saves and reloads preference`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("ui.properties")
        val store = FileThemePreferenceStore(preferenceFile)

        assertEquals(ThemePreferenceLoadResult.Missing, store.load())
        assertEquals(ThemePreferenceSaveResult.Saved, store.save(ThemePreference.DARK))

        val loaded = assertIs<ThemePreferenceLoadResult.Loaded>(store.load())
        assertEquals(ThemePreference.DARK, loaded.preference)
        assertEquals("themePreference=DARK\n", Files.readString(preferenceFile))
    }

    @Test
    fun `save creates parent directories and replaces existing file`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("nested").resolve("preferences").resolve("ui.properties")
        val store = FileThemePreferenceStore(preferenceFile)

        assertEquals(ThemePreferenceSaveResult.Saved, store.save(ThemePreference.LIGHT))
        assertEquals(ThemePreferenceSaveResult.Saved, store.save(ThemePreference.SYSTEM))

        assertTrue(Files.exists(preferenceFile))
        assertEquals("themePreference=SYSTEM\n", Files.readString(preferenceFile))
    }

    @Test
    fun `save falls back to replacement move when atomic move is unavailable`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("ui.properties")
        val moveStrategy = AtomicUnavailableMoveStrategy()
        val store = FileThemePreferenceStore(preferenceFile, ThemePreferenceFileCodec(), moveStrategy)

        assertEquals(ThemePreferenceSaveResult.Saved, store.save(ThemePreference.LIGHT))

        assertEquals(1, moveStrategy.atomicMoveAttempts)
        assertEquals(1, moveStrategy.replaceMoveAttempts)
        assertEquals("themePreference=LIGHT\n", Files.readString(preferenceFile))
    }

    @Test
    fun `load reports invalid value from codec without silently falling back`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("ui.properties")
        Files.writeString(preferenceFile, "themePreference=BLUE\n")

        val invalid = assertIs<ThemePreferenceLoadResult.InvalidValue>(FileThemePreferenceStore(preferenceFile).load())

        assertEquals("BLUE", invalid.rawValue)
    }

    @Test
    fun `write failure removes sibling temp file and redacts storage message`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("ui.properties")
        Files.createDirectory(preferenceFile)
        val store = FileThemePreferenceStore(preferenceFile)

        val failed = assertIs<ThemePreferenceSaveResult.StorageFailed>(store.save(ThemePreference.DARK))

        assertEquals("storage_failed", failed.message)
        val leakedTemps = dir.listDirectoryEntries().filter { it.fileName.toString().contains(".tmp") }
        assertTrue(leakedTemps.isEmpty(), leakedTemps.joinToString())
    }

    @Test
    fun `storage failure is redacted for invalid parent path`() = withTempDirectory { dir ->
        val parentFile = dir.resolve("parent-file")
        Files.writeString(parentFile, "not a directory")
        val store = FileThemePreferenceStore(parentFile.resolve("ui.properties"))

        val failed = assertIs<ThemePreferenceSaveResult.StorageFailed>(store.save(ThemePreference.LIGHT))

        assertEquals("storage_failed", failed.message)
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("hostess-theme-preference-store-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private class AtomicUnavailableMoveStrategy : ThemePreferenceFileMoveStrategy {
        var atomicMoveAttempts: Int = 0
            private set
        var replaceMoveAttempts: Int = 0
            private set

        override fun moveAtomically(
            source: Path,
            target: Path,
        ) {
            atomicMoveAttempts += 1
            throw AtomicMoveNotSupportedException(source.toString(), target.toString(), "synthetic")
        }

        override fun moveReplace(
            source: Path,
            target: Path,
        ) {
            replaceMoveAttempts += 1
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
