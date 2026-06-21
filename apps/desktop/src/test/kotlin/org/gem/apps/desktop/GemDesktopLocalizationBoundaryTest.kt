package org.gem.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse

class GemDesktopLocalizationBoundaryTest {
    @Test
    fun desktopWindowTitleSourceDoesNotReadUiCatalogue() {
        val source = Files.readString(sourcePath(
            "apps/desktop/src/main/kotlin/org/gem/apps/desktop/GemDesktopApp.kt",
            "src/main/kotlin/org/gem/apps/desktop/GemDesktopApp.kt",
        ))

        assertFalse(source.contains("GemTextCatalogue"))
        assertFalse(source.contains("EnglishGemTextCatalogue"))
        assertFalse(source.contains("GemTextKey.BrandInitials"))
    }

    private fun sourcePath(
        rootRelative: String,
        moduleRelative: String,
    ): Path =
        listOf(Path.of(rootRelative), Path.of(moduleRelative))
            .firstOrNull(Files::exists)
            ?: error("source file not found")
}
