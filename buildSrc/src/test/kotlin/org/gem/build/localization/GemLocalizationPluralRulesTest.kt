package org.gem.build.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GemLocalizationPluralRulesTest {
    @Test
    fun returnsExpectedIntegerCategoriesForEveryFormulaFamily() {
        assertCategories("en-GB", 0 to "other", 1 to "one", 2 to "other")
        assertCategories("es-ES", 0 to "other", 1 to "one", 2 to "other", 1_000_000 to "many")
        assertCategories("pt-BR", 0 to "one", 1 to "one", 2 to "other", 1_000_000 to "many")
        assertCategories("pt-PT", 0 to "other", 1 to "one", 2 to "other", 1_000_000 to "many")
        assertCategories("fr-FR", 0 to "one", 1 to "one", 2 to "other", 1_000_000 to "many")
        assertCategories("cs-CZ", 1 to "one", 2 to "few", 4 to "few", 5 to "other")
        assertCategories("lt-LT", 1 to "one", 2 to "few", 10 to "other", 11 to "other", 21 to "one", 22 to "few")
        assertCategories("lv-LV", 0 to "zero", 1 to "one", 2 to "other", 11 to "zero", 21 to "one")
        assertCategories("pl-PL", 1 to "one", 2 to "few", 5 to "many", 12 to "many", 22 to "few", 25 to "many")
        assertCategories("ro-RO", 0 to "few", 1 to "one", 2 to "few", 20 to "other", 100 to "other")
        assertCategories("uk-UA", 1 to "one", 2 to "few", 5 to "many", 11 to "many", 21 to "one", 22 to "few")
    }

    @Test
    fun reportCountsCoverDynamicAuditCases() {
        assertEquals(listOf(0, 1, 2, 3, 5, 21, 22, 25, 249, 1_000_000), GemLocalizationPluralRules.reportCounts)
    }

    @Test
    fun everyApprovedLocaleReturnsOnlySourceBackedCategories() {
        GemLocalizationContract.approvedLocaleTags.forEach { localeTag ->
            GemLocalizationPluralRules.reportCounts.forEach { count ->
                val category = GemLocalizationPluralRules.category(localeTag, count)
                assertTrue(
                    category in GemLocalizationContract.requiredPluralCategoriesByLocale.getValue(localeTag),
                    "Unexpected category <$category> for $localeTag / $count",
                )
            }
        }
    }

    @Test
    fun rejectsNegativeCountAndUnsupportedLocale() {
        assertFailsWith<IllegalArgumentException> {
            GemLocalizationPluralRules.category("en-GB", -1)
        }
        assertFailsWith<IllegalStateException> {
            GemLocalizationPluralRules.category("ga-IE", 1)
        }
    }

    private fun assertCategories(localeTag: String, vararg expectedByCount: Pair<Int, String>) {
        expectedByCount.forEach { (count, expected) ->
            assertEquals(expected, GemLocalizationPluralRules.category(localeTag, count), "$localeTag / $count")
        }
    }
}
