package org.gem.build.localization

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class GemLocalizationPropertiesParserTest {
    @Test
    fun parsesProductionEnglishSource() {
        val source = parseSource(productionEnglishSource())

        assertEquals("en-GB", source.localeTag)
        assertEquals("English", source.languageName)
        assertEquals("English", source.nativeName)
        assertEquals("GEM", source.fixedValues.getValue("BrandInitials"))
        assertEquals("{count} char", source.countValues.getValue("DraftCharCount").getValue("one"))
        assertEquals("{groupName}: {reason}", source.placeholderValues.getValue("SendFailureDetailLine"))
    }

    @Test
    fun rejectsNonEnglishProductionLocale() {
        assertParseFails(
            expectedMessage = "Track D only authorizes en-GB production localization source: fr-FR",
            fileName = "fr-FR.properties",
            text = productionEnglishSource()
                .replaceLine("meta.locale", "meta.locale=fr-FR"),
        )
    }

    @Test
    fun rejectsUnknownMissingAndDuplicateKeys() {
        assertParseFails(
            expectedMessage = "Unknown localization key in en-GB.properties: StrayKey",
            text = productionEnglishSource() + "\nStrayKey=Nope\n",
        )
        assertParseFails(
            expectedMessage = "Missing fixed localization key in en-GB.properties: Login",
            text = productionEnglishSource().removeLine("Login"),
        )
        assertParseFails(
            expectedMessage = "Duplicate localization key in en-GB.properties: Login",
            text = productionEnglishSource() + "\nLogin=Again\n",
        )
    }

    @Test
    fun rejectsProtectedProductPlaceholderAndPluralDrift() {
        assertParseFails(
            expectedMessage = "Protected product localization key changed in en-GB.properties: BrandInitials",
            text = productionEnglishSource().replaceLine("BrandInitials", "BrandInitials=BAD"),
        )
        assertParseFails(
            expectedMessage = "Placeholder mismatch for SendFailureDetailLine in en-GB.properties: expected [groupName, reason]",
            text = productionEnglishSource().replaceLine("SendFailureDetailLine", "SendFailureDetailLine={groupName}"),
        )
        assertParseFails(
            expectedMessage = "Missing localization key in en-GB.properties: DraftCharCount.other",
            text = productionEnglishSource().removeLine("DraftCharCount.other"),
        )
    }

    private fun parseSource(text: String, fileName: String = "en-GB.properties"): GemLocalizationSource {
        val directory = Files.createTempDirectory("gem-localization-parser-test").toFile()
        return try {
            File(directory, fileName).writeText(text)
            GemLocalizationPropertiesParser.parseDirectory(directory).single()
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun assertParseFails(
        expectedMessage: String,
        text: String,
        fileName: String = "en-GB.properties",
    ) {
        val failure = assertFails {
            parseSource(text, fileName)
        }
        assertTrue(
            failure.message.orEmpty().contains(expectedMessage),
            "Expected failure containing <$expectedMessage> but got <${failure.message}>",
        )
    }

    private fun productionEnglishSource(): String {
        val candidates = listOf(
            File("../gem-ui/src/commonMain/localization/en-GB.properties"),
            File("gem-ui/src/commonMain/localization/en-GB.properties"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("production English localization source not found")
    }

    private fun String.replaceLine(key: String, replacement: String): String =
        lines()
            .joinToString("\n") { line ->
                if (line.startsWith("$key=")) replacement else line
            }
            .let { "$it\n" }

    private fun String.removeLine(key: String): String =
        lines()
            .filterNot { it.startsWith("$key=") }
            .joinToString("\n")
            .let { "$it\n" }
}
