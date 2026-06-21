package org.gem.preferences

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult
import org.gem.core.language.LanguagePreferenceSaveResult

class FileLanguagePreferenceStoreTest {
    @Test
    fun loadsMissingFileThenSavesAndReloadsPreference() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("language.properties")
        val store = FileLanguagePreferenceStore(preferenceFile)

        assertEquals(LanguagePreferenceLoadResult.Missing, store.load())
        assertEquals(LanguagePreferenceSaveResult.Saved, store.save(LanguagePreference.Locale("fr-FR")))

        val loaded = assertIs<LanguagePreferenceLoadResult.Loaded>(store.load())
        assertEquals(LanguagePreference.Locale("fr-FR"), loaded.preference)
        assertEquals("languagePreference=LOCALE:fr-FR\n", Files.readString(preferenceFile))
    }

    @Test
    fun saveCreatesParentDirectoriesAndReplacesExistingFile() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("nested").resolve("preferences").resolve("language.properties")
        val store = FileLanguagePreferenceStore(preferenceFile)

        assertEquals(LanguagePreferenceSaveResult.Saved, store.save(LanguagePreference.Locale("uk-UA")))
        assertEquals(LanguagePreferenceSaveResult.Saved, store.save(LanguagePreference.System))

        assertTrue(Files.exists(preferenceFile))
        assertEquals("languagePreference=SYSTEM\n", Files.readString(preferenceFile))
    }

    @Test
    fun saveFallsBackToReplacementMoveWhenAtomicMoveIsUnavailable() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("language.properties")
        val moveStrategy = AtomicUnavailableMoveStrategy()
        val store = FileLanguagePreferenceStore(preferenceFile, LanguagePreferenceFileCodec(), moveStrategy)

        assertEquals(LanguagePreferenceSaveResult.Saved, store.save(LanguagePreference.Locale("de-DE")))

        assertEquals(1, moveStrategy.atomicMoveAttempts)
        assertEquals(1, moveStrategy.replaceMoveAttempts)
        assertEquals("languagePreference=LOCALE:de-DE\n", Files.readString(preferenceFile))
    }

    @Test
    fun loadReportsInvalidValueFromCodecWithoutSilentlyFallingBack() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("language.properties")
        Files.writeString(preferenceFile, "languagePreference=AUTO\n")

        val invalid = assertIs<LanguagePreferenceLoadResult.InvalidValue>(FileLanguagePreferenceStore(preferenceFile).load())

        assertEquals("AUTO", invalid.rawValue)
    }

    @Test
    fun writeFailureRemovesSiblingTempFileAndRedactsStorageMessage() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("language.properties")
        Files.createDirectory(preferenceFile)
        val store = FileLanguagePreferenceStore(preferenceFile)

        val failed = assertIs<LanguagePreferenceSaveResult.StorageFailed>(
            store.save(LanguagePreference.Locale("fr-FR")),
        )

        assertEquals("storage_failed", failed.message)
        val leakedTemps = dir.listDirectoryEntries().filter { it.fileName.toString().contains(".tmp") }
        assertTrue(leakedTemps.isEmpty(), leakedTemps.joinToString())
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("gem-language-preference-store-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private class AtomicUnavailableMoveStrategy : LanguagePreferenceFileMoveStrategy {
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
