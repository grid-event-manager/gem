package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GemLocalizationBoundaryTest {
    @Test
    fun productionRegistryShipsApprovedLocaleSetOnly() {
        assertEquals(ExpectedGemLocalizationLocales.localeTags, GemTextCatalogueRegistry.locales.map { it.localeTag })
        assertSame(EnglishGemTextCatalogue, GemTextCatalogueRegistry.catalogueFor("en-GB"))
        assertEquals(null, GemTextCatalogueRegistry.catalogueFor("ga-IE"))
    }
}
