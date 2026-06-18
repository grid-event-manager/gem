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
            listOf("alpha", "Beta", "gamma", "sans-serif"),
            catalogue.availableFamilies().map { it.value },
        )
    }

    @Test
    fun `appends one canonical sans serif alias`() {
        val catalogue = JvmPlatformFontCatalogue {
            arrayOf("Sans-Serif", "Roboto")
        }

        assertEquals(
            listOf("Roboto", "sans-serif"),
            catalogue.availableFamilies().map { it.value },
        )
    }

    @Test
    fun `empty desktop catalogue still exposes sans serif`() {
        val catalogue = JvmPlatformFontCatalogue {
            emptyArray()
        }

        assertEquals(listOf("sans-serif"), catalogue.availableFamilies().map { it.value })
    }
}
