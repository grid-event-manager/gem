package org.gem.preferences

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.listDirectoryEntries
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSaveResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FileAppearanceProfileStoreTest {
    @Test
    fun `loads missing file then saves and reloads snapshot`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("appearance-profiles.properties")
        val store = FileAppearanceProfileStore(preferenceFile)
        val snapshot = snapshot()

        assertEquals(AppearanceProfileStoreLoadResult.Missing, store.load())
        assertEquals(AppearanceProfileStoreSaveResult.Saved, store.save(snapshot))

        val loaded = assertIs<AppearanceProfileStoreLoadResult.Loaded>(store.load())
        assertEquals(snapshot, loaded.snapshot)
        assertTrue(Files.readString(preferenceFile).startsWith("formatVersion=1\nactiveProfile.light="))
    }

    @Test
    fun `save creates parent directories and replaces existing profile file`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("nested").resolve("preferences").resolve("appearance-profiles.properties")
        val store = FileAppearanceProfileStore(preferenceFile)

        assertEquals(AppearanceProfileStoreSaveResult.Saved, store.save(snapshot()))
        assertEquals(AppearanceProfileStoreSaveResult.Saved, store.save(snapshot(activeLightId = null)))

        assertTrue(Files.exists(preferenceFile))
        val loaded = assertIs<AppearanceProfileStoreLoadResult.Loaded>(store.load())
        assertEquals(null, loaded.snapshot.activeLightProfileId)
    }

    @Test
    fun `save falls back to replacement move when atomic move is unavailable`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("appearance-profiles.properties")
        val moveStrategy = AtomicUnavailableMoveStrategy()
        val store = FileAppearanceProfileStore(preferenceFile, AppearanceProfileFileCodec(), moveStrategy)

        assertEquals(AppearanceProfileStoreSaveResult.Saved, store.save(snapshot()))

        assertEquals(1, moveStrategy.atomicMoveAttempts)
        assertEquals(1, moveStrategy.replaceMoveAttempts)
    }

    @Test
    fun `invalid profile file is returned as invalid and left untouched`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("appearance-profiles.properties")
        val invalidText = "formatVersion=1\nactiveProfile.light=\nactiveProfile.dark=\nprofile.0.id=\n"
        Files.writeString(preferenceFile, invalidText)

        val invalid = assertIs<AppearanceProfileStoreLoadResult.Invalid>(FileAppearanceProfileStore(preferenceFile).load())

        assertEquals("blank_profile_id", invalid.reason)
        assertEquals(invalidText, Files.readString(preferenceFile))
    }

    @Test
    fun `load reports storage failure for unreadable profile path`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("appearance-profiles.properties")
        Files.createDirectory(preferenceFile)

        val failed = assertIs<AppearanceProfileStoreLoadResult.StorageFailed>(
            FileAppearanceProfileStore(preferenceFile).load(),
        )

        assertEquals("storage_failed", failed.message)
    }

    @Test
    fun `write failure removes sibling temp file and redacts storage message`() = withTempDirectory { dir ->
        val preferenceFile = dir.resolve("appearance-profiles.properties")
        Files.createDirectory(preferenceFile)
        val store = FileAppearanceProfileStore(preferenceFile)

        val failed = assertIs<AppearanceProfileStoreSaveResult.StorageFailed>(store.save(snapshot()))

        assertEquals("storage_failed", failed.message)
        val leakedTemps = dir.listDirectoryEntries().filter { it.fileName.toString().contains(".tmp") }
        assertTrue(leakedTemps.isEmpty(), leakedTemps.joinToString())
    }

    private fun snapshot(
        activeLightId: AppearanceProfileId? = AppearanceProfileId("custom:light:venue"),
    ): AppearanceProfileStoreSnapshot =
        AppearanceProfileStoreSnapshot(
            customProfiles = listOf(customProfile()),
            activeLightProfileId = activeLightId,
            activeDarkProfileId = null,
        )

    private fun customProfile(): AppearanceProfile {
        val draft = AppearanceDraft.fromProfile(
            AppearanceProfileCatalogue.stockProfiles().first { it.mode == AppearanceMode.LIGHT },
        )
        return AppearanceProfile(
            id = AppearanceProfileId("custom:light:venue"),
            name = AppearanceProfileName("Venue"),
            mode = AppearanceMode.LIGHT,
            source = AppearanceProfileSource.CUSTOM,
            textFonts = draft.textFonts,
            textColors = draft.textColors,
            elementColors = draft.elementColors,
        )
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("gem-appearance-profile-store-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private class AtomicUnavailableMoveStrategy : AppearanceProfileFileMoveStrategy {
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
