package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.gem.core.language.LanguagePreference

class GemTextCatalogueResolverTest {
    private val registry = GemTextCatalogueRegistry.generated()

    @Test
    fun exactSystemLocaleMatchesUseGeneratedCataloguesWithoutWarning() {
        listOf(
            "fr-FR" to "Connexion",
            "pt-BR" to "Entrar",
            "pt-PT" to "Iniciar sessão",
            "uk-UA" to "Увійти",
        ).forEach { (localeTag, expectedLogin) ->
            val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, localeTag, registry)

            assertEquals(LanguagePreference.System, selection.preference)
            assertResolvedSelection(
                selection = selection,
                requestedLocaleTag = localeTag,
                resolvedLocaleTag = localeTag,
                warningKey = null,
                expectedLogin = expectedLogin,
            )
        }
    }

    @Test
    fun languageSubtagMatchesUseFirstSortedGeneratedLocaleWithoutWarning() {
        listOf(
            Triple("fr-CA", "fr-FR", "Connexion"),
            Triple("pt-AO", "pt-BR", "Entrar"),
            Triple("de-AT", "de-DE", "Anmelden"),
            Triple("en-US", "en-GB", "Login"),
        ).forEach { (requestedLocaleTag, resolvedLocaleTag, expectedLogin) ->
            val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, requestedLocaleTag, registry)

            assertResolvedSelection(
                selection = selection,
                requestedLocaleTag = requestedLocaleTag,
                resolvedLocaleTag = resolvedLocaleTag,
                warningKey = null,
                expectedLogin = expectedLogin,
            )
        }
    }

    @Test
    fun unsupportedSystemLocaleFallsBackToEnglishWithoutWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.System, "zz-ZZ", registry)

        assertResolvedSelection(
            selection = selection,
            requestedLocaleTag = "zz-ZZ",
            resolvedLocaleTag = "en-GB",
            warningKey = null,
            expectedLogin = "Login",
        )
        assertSame(EnglishGemTextCatalogue, selection.catalogue)
    }

    @Test
    fun exactManualLocaleMatchWinsOverSystemLocale() {
        val preference = LanguagePreference.Locale("fr-FR")
        val selection = GemTextCatalogueResolver.resolve(preference, "en-US", registry)

        assertEquals(preference, selection.preference)
        assertResolvedSelection(
            selection = selection,
            requestedLocaleTag = "fr-FR",
            resolvedLocaleTag = "fr-FR",
            warningKey = null,
            expectedLogin = "Connexion",
        )
    }

    @Test
    fun unsupportedManualLocaleFallsBackToEnglishWithWarning() {
        val selection = GemTextCatalogueResolver.resolve(LanguagePreference.Locale("zz-ZZ"), "fr-FR", registry)

        assertResolvedSelection(
            selection = selection,
            requestedLocaleTag = "zz-ZZ",
            resolvedLocaleTag = "en-GB",
            warningKey = GemTextKey.LanguagePreferenceUnavailable,
            expectedLogin = "Login",
        )
        assertSame(EnglishGemTextCatalogue, selection.catalogue)
    }

    private fun assertResolvedSelection(
        selection: GemTextCatalogueSelection,
        requestedLocaleTag: String,
        resolvedLocaleTag: String,
        warningKey: GemTextKey?,
        expectedLogin: String,
    ) {
        assertEquals(requestedLocaleTag, selection.requestedLocaleTag)
        assertEquals(resolvedLocaleTag, selection.resolvedLocaleTag)
        assertEquals(warningKey, selection.warningKey)
        assertEquals(expectedLogin, selection.catalogue.text(GemTextKey.Login))
        assertTrue(selection.catalogue.text(GemTextKey.DraftCharCount(2)).contains("2"))
        assertTrue(selection.catalogue.text(GemTextKey.SelectedCount(2)).contains("2"))
    }
}
