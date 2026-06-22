package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GemPluralCategoryResolverTest {
    @Test
    fun resolvesRepresentativeApprovedIntegerPluralCategories() {
        assertCategories("en-GB", 0 to GemPluralCategory.OTHER, 1 to GemPluralCategory.ONE, 2 to GemPluralCategory.OTHER)
        assertCategories("fr-FR", 0 to GemPluralCategory.ONE, 1 to GemPluralCategory.ONE, 2 to GemPluralCategory.OTHER, 1_000_000 to GemPluralCategory.MANY)
        assertCategories("pt-BR", 0 to GemPluralCategory.ONE, 1 to GemPluralCategory.ONE, 2 to GemPluralCategory.OTHER, 1_000_000 to GemPluralCategory.MANY)
        assertCategories("pt-PT", 0 to GemPluralCategory.OTHER, 1 to GemPluralCategory.ONE, 2 to GemPluralCategory.OTHER, 1_000_000 to GemPluralCategory.MANY)
        assertCategories("uk-UA", 1 to GemPluralCategory.ONE, 2 to GemPluralCategory.FEW, 5 to GemPluralCategory.MANY, 21 to GemPluralCategory.ONE)
    }

    @Test
    fun unsupportedLocaleFailsExplicitly() {
        assertFailsWith<IllegalStateException> {
            GemPluralCategoryResolver.category("ga-IE", 1)
        }
    }

    @Test
    fun negativeCountFailsBeforeLanguageBranching() {
        assertFailsWith<IllegalArgumentException> {
            GemPluralCategoryResolver.category("en-GB", -1)
        }
    }

    private fun assertCategories(localeTag: String, vararg expectedByCount: Pair<Int, GemPluralCategory>) {
        expectedByCount.forEach { (count, expected) ->
            assertEquals(expected, GemPluralCategoryResolver.category(localeTag, count), "$localeTag / $count")
        }
    }
}
