package org.gem.preferences

import java.nio.file.Files
import java.util.Comparator
import org.gem.core.domain.AccountProfileId
import org.gem.core.preferences.LastLoginProfilePreferenceLoadResult
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FileLastLoginProfilePreferenceStoreTest {
    @Test
    fun `round trips profile id through atomic preference file`() {
        val directory = Files.createTempDirectory("gem-last-login-profile-test")
        try {
            val profileId = AccountProfileId("profile:v1:jack")
            val store = FileLastLoginProfilePreferenceStore(directory.resolve("last-login-profile.txt"))

            assertEquals(LastLoginProfilePreferenceSaveResult.Saved, store.save(profileId))
            assertEquals(LastLoginProfilePreferenceLoadResult.Loaded(profileId), store.load())
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `missing and invalid values are explicit`() {
        val directory = Files.createTempDirectory("gem-last-login-profile-invalid-test")
        try {
            val preferenceFile = directory.resolve("last-login-profile.txt")
            val store = FileLastLoginProfilePreferenceStore(preferenceFile)

            assertEquals(LastLoginProfilePreferenceLoadResult.Missing, store.load())

            Files.writeString(preferenceFile, "not-a-profile-id")

            assertIs<LastLoginProfilePreferenceLoadResult.InvalidValue>(store.load())
        } finally {
            deleteRecursively(directory)
        }
    }

    private fun deleteRecursively(path: java.nio.file.Path) {
        if (!Files.exists(path)) {
            return
        }
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}
