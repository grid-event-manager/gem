package org.gem.ui.runtime

import org.gem.ui.text.GemTextCatalogueResolver
import org.gem.ui.text.GemTextCatalogueSelection

object GemStartupLanguageSelection {
    fun initial(runtime: GemUiRuntime): GemTextCatalogueSelection =
        GemTextCatalogueResolver.resolve(
            preference = runtime.languagePreferenceService.loadPreference().preference,
            systemLocaleTag = runtime.platformLocaleProvider.currentLocaleTag(),
        )
}
