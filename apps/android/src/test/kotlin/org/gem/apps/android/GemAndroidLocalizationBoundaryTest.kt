package org.gem.apps.android

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse

class GemAndroidLocalizationBoundaryTest {
    @Test
    fun androidDoesNotRouteCommonUiCopyThroughStringsXml() {
        val valuesDir = sourcePath(
            "apps/android/src/main/res/values",
            "src/main/res/values",
        )
        val androidStringResource = "strings" + ".xml"

        assertFalse(Files.exists(valuesDir.resolve(androidStringResource)))
    }

    private fun sourcePath(
        rootRelative: String,
        moduleRelative: String,
    ): Path =
        listOf(Path.of(rootRelative), Path.of(moduleRelative))
            .firstOrNull(Files::exists)
            ?: error("source directory not found")
}
