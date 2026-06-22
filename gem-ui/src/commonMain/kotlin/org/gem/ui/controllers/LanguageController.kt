package org.gem.ui.controllers

import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadWarning
import org.gem.core.language.LanguagePreferenceSaveResult
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.LanguageOption
import org.gem.ui.state.LanguageUiState
import org.gem.ui.text.GemTextCatalogueRegistry
import org.gem.ui.text.GemTextCatalogueResolver
import org.gem.ui.text.GemTextCatalogueSelection
import org.gem.ui.text.GemTextKey

class LanguageController(
    private val runtime: GemUiRuntime,
    private val registry: GemTextCatalogueRegistry,
    val state: LanguageUiState,
) {
    fun selectOption(option: LanguageOption): LanguageSelectionChange {
        val preference = option.preference ?: return LanguageSelectionChange(this, null)
        val saveResult = runtime.languagePreferenceService.savePreference(preference)
        val selection = resolve(preference)
        val warningKey = when (saveResult) {
            LanguagePreferenceSaveResult.Saved -> selection.warningKey
            is LanguagePreferenceSaveResult.StorageFailed -> GemTextKey.LanguagePreferenceSaveFailed
        }
        return LanguageSelectionChange(
            controller = copy(state = stateFrom(selection, warningKey = warningKey)),
            selection = selection,
        )
    }

    private fun resolve(preference: LanguagePreference): GemTextCatalogueSelection =
        GemTextCatalogueResolver.resolve(
            preference = preference,
            systemLocaleTag = runtime.platformLocaleProvider.currentLocaleTag(),
            registry = registry,
        )

    private fun stateFrom(
        selection: GemTextCatalogueSelection,
        warningKey: GemTextKey?,
    ): LanguageUiState =
        state.copy(
            preference = selection.preference,
            requestedLocaleTag = selection.requestedLocaleTag,
            resolvedLocaleTag = selection.resolvedLocaleTag,
            warningKey = warningKey,
            options = options(registry),
        )

    private fun copy(state: LanguageUiState): LanguageController =
        LanguageController(runtime, registry, state)

    companion object {
        fun initial(
            runtime: GemUiRuntime,
            activeSelection: GemTextCatalogueSelection,
            registry: GemTextCatalogueRegistry = GemTextCatalogueRegistry.generated(),
        ): LanguageController {
            val loadWarningKey = runtime.languagePreferenceService.loadPreference().warning?.toTextKey()
            val warningKey = loadWarningKey ?: activeSelection.warningKey
            return LanguageController(
                runtime = runtime,
                registry = registry,
                state = LanguageUiState(
                    preference = activeSelection.preference,
                    requestedLocaleTag = activeSelection.requestedLocaleTag,
                    resolvedLocaleTag = activeSelection.resolvedLocaleTag,
                    warningKey = warningKey,
                    options = options(registry),
                ),
            )
        }

        private fun options(registry: GemTextCatalogueRegistry): List<LanguageOption> =
            listOf(LanguageOption.Placeholder, LanguageOption.System) +
                registry.locales.map { metadata ->
                    LanguageOption.Locale(
                        localeTag = metadata.localeTag,
                        nativeName = metadata.nativeName,
                    )
                }

        private fun LanguagePreferenceLoadWarning.toTextKey(): GemTextKey =
            GemTextKey.LanguagePreferenceUnavailable
    }
}

data class LanguageSelectionChange(
    val controller: LanguageController,
    val selection: GemTextCatalogueSelection?,
)
