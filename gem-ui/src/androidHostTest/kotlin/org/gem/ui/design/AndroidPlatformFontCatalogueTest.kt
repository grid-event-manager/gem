package org.gem.ui.design

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidPlatformFontCatalogueTest {
    @Test
    fun `uses fixed android font list before system font api`() {
        val catalogue = AndroidPlatformFontCatalogue(
            sdkInt = { 28 },
            availableFontNames = { error("system fonts must not be read before API 29") },
        )

        assertEquals(
            listOf(
                "casual",
                "cursive",
                "monospace",
                "sans",
                "sans-serif",
                "sans-serif-black",
                "sans-serif-condensed",
                "sans-serif-light",
                "sans-serif-medium",
                "serif",
                "serif-monospace",
            ),
            catalogue.availableFamilies().map { it.value },
        )
    }

    @Test
    fun `normalizes system font names from API 29 onwards`() {
        val catalogue = AndroidPlatformFontCatalogue(
            sdkInt = { 29 },
            availableFontNames = { listOf(" Roboto ", "", "roboto", "NotoSans-Bold") },
        )

        assertEquals(
            listOf("NotoSans-Bold", "Roboto"),
            catalogue.availableFamilies().map { it.value },
        )
    }
}
