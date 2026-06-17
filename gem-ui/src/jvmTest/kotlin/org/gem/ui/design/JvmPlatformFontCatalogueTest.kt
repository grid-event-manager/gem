package org.gem.ui.design

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmPlatformFontCatalogueTest {
    @Test
    fun `normalizes desktop font family names`() {
        val catalogue = JvmPlatformFontCatalogue {
            arrayOf(" Beta ", "", "alpha", "Alpha", "gamma", "beta")
        }

        assertEquals(
            listOf("alpha", "Beta", "gamma"),
            catalogue.availableFamilies().map { it.value },
        )
    }
}
