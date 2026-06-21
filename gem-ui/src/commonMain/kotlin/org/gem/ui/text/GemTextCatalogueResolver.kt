package org.gem.ui.text

import org.gem.core.language.LanguagePreference

object GemTextCatalogueResolver {
    fun resolve(
        preference: LanguagePreference,
        systemLocaleTag: String,
        registry: GemTextCatalogueRegistry = GemTextCatalogueRegistry.generated(),
    ): GemTextCatalogueSelection {
        val requestedLocaleTag = when (preference) {
            is LanguagePreference.Locale -> preference.localeTag
            LanguagePreference.System -> systemLocaleTag
        }
        val exact = registry.catalogueMetadataFor(requestedLocaleTag)
        if (exact != null) {
            return selection(preference, requestedLocaleTag, exact, warningKey = null)
        }

        val languageMatch = registry.locales.firstOrNull { metadata ->
            metadata.localeTag.languageSubtag() == requestedLocaleTag.languageSubtag()
        }
        if (languageMatch != null) {
            return selection(preference, requestedLocaleTag, languageMatch, warningKey = null)
        }

        val fallback = registry.catalogueMetadataFor(FALLBACK_LOCALE_TAG)
            ?: error("Missing required $FALLBACK_LOCALE_TAG text catalogue")
        val warningKey = when (preference) {
            is LanguagePreference.Locale -> GemTextKey.LanguagePreferenceUnavailable
            LanguagePreference.System -> null
        }
        return selection(preference, requestedLocaleTag, fallback, warningKey)
    }

    private fun selection(
        preference: LanguagePreference,
        requestedLocaleTag: String,
        metadata: GemTextCatalogueMetadata,
        warningKey: GemTextKey?,
    ): GemTextCatalogueSelection =
        GemTextCatalogueSelection(
            preference = preference,
            requestedLocaleTag = requestedLocaleTag,
            resolvedLocaleTag = metadata.localeTag,
            catalogue = metadata.catalogue,
            warningKey = warningKey,
        )

    private fun GemTextCatalogueRegistry.catalogueMetadataFor(localeTag: String): GemTextCatalogueMetadata? =
        locales.firstOrNull { it.localeTag == localeTag }

    private fun String.languageSubtag(): String =
        substringBefore('-').lowercase()

    private const val FALLBACK_LOCALE_TAG: String = "en-GB"
}
