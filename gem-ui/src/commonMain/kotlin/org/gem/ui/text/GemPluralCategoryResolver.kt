package org.gem.ui.text

object GemPluralCategoryResolver {
    fun category(localeTag: String, count: Int): GemPluralCategory {
        val language = localeTag.substringBefore('-').lowercase()
        return when (language) {
            "en" -> if (count == 1) GemPluralCategory.ONE else GemPluralCategory.OTHER
            else -> error("Unsupported plural locale: $localeTag")
        }
    }
}
