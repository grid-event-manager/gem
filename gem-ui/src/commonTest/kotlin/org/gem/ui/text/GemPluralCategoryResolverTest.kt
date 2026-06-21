package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GemPluralCategoryResolverTest {
    @Test
    fun englishUsesOneOnlyForIntegerOne() {
        assertEquals(GemPluralCategory.OTHER, GemPluralCategoryResolver.category("en-GB", 0))
        assertEquals(GemPluralCategory.ONE, GemPluralCategoryResolver.category("en-GB", 1))
        assertEquals(GemPluralCategory.OTHER, GemPluralCategoryResolver.category("en-GB", 2))
    }

    @Test
    fun unsupportedLocaleFailsUntilRfcAddsPluralFormula() {
        assertFailsWith<IllegalStateException> {
            GemPluralCategoryResolver.category("fr-FR", 1)
        }
    }
}
