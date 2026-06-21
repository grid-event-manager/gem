package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.gem.core.language.LanguagePreference

class GemTextCatalogueResolverTest {
    private val registry = GemTextCatalogueRegistry(
        listOf(
            GemTextCatalogueMetadata("fr-FR", "French", "Francais", ExactLocaleCatalogue),
            GemTextCatalogueMetadata("en-GB", "English", "English", EnglishGemTextCatalogue),
            GemTextCatalogueMetadata("fr-CA", "French", "Francais (Canada)", LanguageFallbackCatalogue),
        ),
    )

    @Test
    fun exactSystemLocaleMatchReturnsMatchingCatalogueWithoutWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, "fr-FR", registry)

        assertEquals(LanguagePreference.System, selection.preference)
        assertEquals("fr-FR", selection.requestedLocaleTag)
        assertEquals("fr-FR", selection.resolvedLocaleTag)
        assertSame(ExactLocaleCatalogue, selection.catalogue)
        assertEquals(null, selection.warningKey)
    }

    @Test
    fun languageOnlySystemLocaleMatchUsesFirstSortedLocaleWithoutWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, "fr-BE", registry)

        assertEquals("fr-BE", selection.requestedLocaleTag)
        assertEquals("fr-CA", selection.resolvedLocaleTag)
        assertSame(LanguageFallbackCatalogue, selection.catalogue)
        assertEquals(null, selection.warningKey)
    }

    @Test
    fun unsupportedSystemLocaleFallsBackToEnglishWithoutWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, "zz-ZZ", registry)

        assertEquals("zz-ZZ", selection.requestedLocaleTag)
        assertEquals("en-GB", selection.resolvedLocaleTag)
        assertSame(EnglishGemTextCatalogue, selection.catalogue)
        assertEquals(null, selection.warningKey)
    }

    @Test
    fun exactManualLocaleMatchWinsOverSystemLocale() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.Locale("fr-FR"), "en-US", registry)

        assertEquals(LanguagePreference.Locale("fr-FR"), selection.preference)
        assertEquals("fr-FR", selection.requestedLocaleTag)
        assertEquals("fr-FR", selection.resolvedLocaleTag)
        assertSame(ExactLocaleCatalogue, selection.catalogue)
        assertEquals(null, selection.warningKey)
    }

    @Test
    fun unsupportedManualLocaleFallsBackToEnglishWithWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.Locale("zz-ZZ"), "fr-FR", registry)

        assertEquals("zz-ZZ", selection.requestedLocaleTag)
        assertEquals("en-GB", selection.resolvedLocaleTag)
        assertSame(EnglishGemTextCatalogue, selection.catalogue)
        assertEquals(GemTextKey.LanguagePreferenceUnavailable, selection.warningKey)
    }

    private object ExactLocaleCatalogue : GemTextCatalogue {
        override fun text(key: GemTextKey): String =
            "fr-FR:${EnglishGemTextCatalogue.text(key)}"
    }

    private object LanguageFallbackCatalogue : GemTextCatalogue {
        override fun text(key: GemTextKey): String =
            "fr-CA:${EnglishGemTextCatalogue.text(key)}"
    }
}
