package org.gem.build.localization

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class GemLocalizationProductConsumerTest {
    @Test
    fun firstDynamicTextConsumersUseCatalogueKeys() {
        val noticeEditor = sourceFile("gem-ui/src/commonMain/kotlin/org/gem/ui/components/NoticeEditor.kt").readText()
        val groupTargetSelector = sourceFile("gem-ui/src/commonMain/kotlin/org/gem/ui/components/GroupTargetSelector.kt").readText()

        assertTrue(
            noticeEditor.contains("textCatalogue.text(GemTextKey.DraftCharCount(state.charCount))"),
            "NoticeEditor must render draft count through the catalogue dynamic key",
        )
        assertTrue(
            groupTargetSelector.contains("textCatalogue.text(GemTextKey.SelectedCount(state.selectedCount))"),
            "GroupTargetSelector must render selected count through the catalogue dynamic key",
        )
    }

    private fun sourceFile(relativePath: String): File {
        val candidates = listOf(
            File("../$relativePath"),
            File(relativePath),
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Source file not found: $relativePath")
    }
}
