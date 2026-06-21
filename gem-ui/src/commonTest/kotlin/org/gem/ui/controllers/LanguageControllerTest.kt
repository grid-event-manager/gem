package org.gem.ui.controllers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult
import org.gem.core.language.LanguagePreferenceSaveResult
import org.gem.ui.state.LanguageOption
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeLanguagePreferenceStore
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextCatalogueMetadata
import org.gem.ui.text.GemTextCatalogueRegistry
import org.gem.ui.text.GemTextCatalogueResolver
import org.gem.ui.text.GemTextKey

class LanguageControllerTest {
    @Test
    fun initialStateIsCollapsedAndListsSystemThenLocalesSortedByTag() {
        val controller = controller()

        assertFalse(controller.state.expanded)
        assertEquals(
            listOf(
                LanguageOption.Placeholder,
                LanguageOption.System,
                LanguageOption.Locale("en-GB", "English"),
                LanguageOption.Locale("fr-FR", "Francais"),
            ),
            controller.state.options,
        )
        assertEquals(LanguagePreference.System, controller.state.preference)
        assertEquals("en-GB", controller.state.resolvedLocaleTag)
        assertNull(controller.state.warningKey)
    }

    @Test
    fun placeholderSelectionDoesNotSaveOrChangeActiveSelection() {
        val store = FakeLanguagePreferenceStore()
        val controller = controller(store = store)

        val changed = controller.selectOption(LanguageOption.Placeholder)

        assertSame(controller, changed.controller)
        assertNull(changed.selection)
        assertNull(store.lastSavedPreference)
    }

    @Test
    fun selectingLocaleSavesPreferenceAndReturnsNewCatalogueSelection() {
        val store = FakeLanguagePreferenceStore()
        val controller = controller(store = store)

        val changed = controller.selectOption(LanguageOption.Locale("fr-FR", "Francais"))
        val selection = assertNotNull(changed.selection)

        assertEquals(LanguagePreference.Locale("fr-FR"), store.lastSavedPreference)
        assertEquals(LanguagePreference.Locale("fr-FR"), changed.controller.state.preference)
        assertEquals("fr-FR", changed.controller.state.resolvedLocaleTag)
        assertSame(FrenchCatalogue, selection.catalogue)
        assertEquals(
            "fr-FR:${EnglishGemTextCatalogue.text(GemTextKey.Settings)}",
            selection.catalogue.text(GemTextKey.Settings),
        )
    }

    @Test
    fun selectingSystemSavesSystemPreferenceAndResolvesPlatformLocale() {
        val store = FakeLanguagePreferenceStore()
        val controller = controller(
            store = store,
            systemLocaleTag = "fr-CA",
        )

        val changed = controller.selectOption(LanguageOption.System)

        assertEquals(LanguagePreference.System, store.lastSavedPreference)
        assertEquals("fr-CA", changed.selection?.requestedLocaleTag)
        assertEquals("fr-FR", changed.selection?.resolvedLocaleTag)
        assertSame(FrenchCatalogue, changed.selection?.catalogue)
    }

    @Test
    fun saveFailureStillReturnsImmediateSelectionAndShowsSaveWarning() {
        val controller = controller(
            store = FakeLanguagePreferenceStore(saveResult = LanguagePreferenceSaveResult.StorageFailed()),
        )

        val changed = controller.selectOption(LanguageOption.Locale("fr-FR", "Francais"))

        assertSame(FrenchCatalogue, changed.selection?.catalogue)
        assertEquals(GemTextKey.LanguagePreferenceSaveFailed, changed.controller.state.warningKey)
    }

    @Test
    fun loadWarningsAndUnsupportedPersistedLocaleUseUnavailableWarning() {
        val invalid = controller(
            store = FakeLanguagePreferenceStore(
                loadResult = LanguagePreferenceLoadResult.InvalidValue("???"),
            ),
        )
        assertEquals(GemTextKey.LanguagePreferenceUnavailable, invalid.state.warningKey)

        val storageFailed = controller(
            store = FakeLanguagePreferenceStore(
                loadResult = LanguagePreferenceLoadResult.StorageFailed(),
            ),
        )
        assertEquals(GemTextKey.LanguagePreferenceUnavailable, storageFailed.state.warningKey)

        val unsupportedSelection = GemTextCatalogueResolver.resolve(
            preference = LanguagePreference.Locale("zz-ZZ"),
            systemLocaleTag = "en-GB",
            registry = registry,
        )
        val unsupported = LanguageController.initial(
            runtime = FakeGemUiRuntime.ready(),
            activeSelection = unsupportedSelection,
            registry = registry,
        )
        assertEquals(GemTextKey.LanguagePreferenceUnavailable, unsupported.state.warningKey)
    }

    private fun controller(
        store: FakeLanguagePreferenceStore = FakeLanguagePreferenceStore(),
        systemLocaleTag: String = "en-GB",
    ): LanguageController {
        val runtime = FakeGemUiRuntime.ready(
            languagePreferenceStore = store,
            platformLocaleProvider = { systemLocaleTag },
        )
        return LanguageController.initial(
            runtime = runtime,
            activeSelection = GemTextCatalogueResolver.resolve(
                preference = LanguagePreference.System,
                systemLocaleTag = systemLocaleTag,
                registry = registry,
            ),
            registry = registry,
        )
    }

    private object FrenchCatalogue : GemTextCatalogue {
        override fun text(key: GemTextKey): String =
            "fr-FR:${EnglishGemTextCatalogue.text(key)}"
    }

    private companion object {
        val registry = GemTextCatalogueRegistry(
            listOf(
                GemTextCatalogueMetadata("fr-FR", "French", "Francais", FrenchCatalogue),
                GemTextCatalogueMetadata("en-GB", "English", "English", EnglishGemTextCatalogue),
            ),
        )
    }
}
