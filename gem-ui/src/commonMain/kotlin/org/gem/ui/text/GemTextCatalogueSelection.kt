package org.gem.ui.text

import org.gem.core.language.LanguagePreference

data class GemTextCatalogueSelection(
    val preference: LanguagePreference,
    val requestedLocaleTag: String,
    val resolvedLocaleTag: String,
    val catalogue: GemTextCatalogue,
    val warningKey: GemTextKey?,
)
