package org.gem.preferences

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class LegacyDesktopPreferenceMigrationTest {
    @Test
    fun `moves legacy Hostess preferences directory to gem preferences directory`() {
        withTempDataHome { dataHome ->
            val legacyPreferences = dataHome.resolve("Hostess/preferences")
            val canonicalPreferences = dataHome.resolve("gem/preferences")
            Files.createDirectories(legacyPreferences.resolve("inventory-snapshots"))
            Files.writeString(legacyPreferences.resolve("ui.properties"), "theme=dark")
            Files.writeString(legacyPreferences.resolve("last-login-profile.txt"), "profile:v1:last")
            Files.writeString(legacyPreferences.resolve("inventory-snapshots/cache.json"), "{}")

            val result = LegacyDesktopPreferenceMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopPreferenceMigrationResult.Moved>(result)
            assertFalse(Files.exists(legacyPreferences))
            assertEquals("theme=dark", Files.readString(canonicalPreferences.resolve("ui.properties")))
            assertEquals(
                "profile:v1:last",
                Files.readString(canonicalPreferences.resolve("last-login-profile.txt")),
            )
            assertEquals("{}", Files.readString(canonicalPreferences.resolve("inventory-snapshots/cache.json")))
        }
    }

    @Test
    fun `keeps canonical gem preferences authoritative when both paths exist`() {
        withTempDataHome { dataHome ->
            val legacyPreferences = dataHome.resolve("Hostess/preferences")
            val canonicalPreferences = dataHome.resolve("gem/preferences")
            Files.createDirectories(legacyPreferences)
            Files.createDirectories(canonicalPreferences)
            Files.writeString(legacyPreferences.resolve("ui.properties"), "theme=legacy")
            Files.writeString(canonicalPreferences.resolve("ui.properties"), "theme=gem")

            val result = LegacyDesktopPreferenceMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopPreferenceMigrationResult.CanonicalAlreadyPresent>(result)
            assertEquals("theme=legacy", Files.readString(legacyPreferences.resolve("ui.properties")))
            assertEquals("theme=gem", Files.readString(canonicalPreferences.resolve("ui.properties")))
        }
    }

    @Test
    fun `returns no legacy data when old preferences path is absent`() {
        withTempDataHome { dataHome ->
            val result = LegacyDesktopPreferenceMigration.run(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to dataHome.toString()),
                userHome = dataHome.resolve("home").toString(),
            )

            assertIs<LegacyDesktopPreferenceMigrationResult.NoLegacyData>(result)
            assertFalse(Files.exists(dataHome.resolve("gem/preferences")))
        }
    }

    @Test
    fun `falls back to non atomic preference move when atomic move is unsupported`() {
        withTempDataHome { dataHome ->
            val legacyPreferences = dataHome.resolve("Hostess/preferences")
            val canonicalPreferences = dataHome.resolve("gem/preferences")
            Files.createDirectories(legacyPreferences)
            Files.writeString(legacyPreferences.resolve("ui.properties"), "theme=system")

            val result = LegacyDesktopPreferenceMigration.migratePath(
                legacyPath = legacyPreferences,
                canonicalPath = canonicalPreferences,
                atomicMove = { _, _ ->
                    throw AtomicMoveNotSupportedException("legacy", "canonical", "unsupported")
                },
                fallbackMove = { source, target ->
                    Files.move(source, target)
                },
            )

            assertIs<LegacyDesktopPreferenceMigrationResult.Moved>(result)
            assertFalse(Files.exists(legacyPreferences))
            assertEquals("theme=system", Files.readString(canonicalPreferences.resolve("ui.properties")))
        }
    }

    @Test
    fun `fails closed when preference move fails`() {
        withTempDataHome { dataHome ->
            val legacyPreferences = dataHome.resolve("Hostess/preferences")
            val canonicalPreferences = dataHome.resolve("gem/preferences")
            Files.createDirectories(legacyPreferences)
            Files.writeString(legacyPreferences.resolve("ui.properties"), "theme=light")

            assertFailsWith<LegacyDesktopPreferenceMigrationException> {
                LegacyDesktopPreferenceMigration.migratePath(
                    legacyPath = legacyPreferences,
                    canonicalPath = canonicalPreferences,
                    atomicMove = { _, _ -> throw IOException("denied") },
                )
            }

            assertTrue(Files.exists(legacyPreferences.resolve("ui.properties")))
            assertFalse(Files.exists(canonicalPreferences))
        }
    }

    private fun withTempDataHome(block: (Path) -> Unit) {
        val dataHome = Files.createTempDirectory("gem-preference-migration-test")
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
