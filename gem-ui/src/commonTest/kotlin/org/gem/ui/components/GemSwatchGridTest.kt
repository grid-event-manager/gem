package org.gem.ui.components

import org.gem.ui.design.AppearanceSwatchPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class GemSwatchGridTest {
    @Test
    fun swatchesMatchPrototypePaletteAndOrder() {
        assertEquals(80, AppearanceSwatchPalette.colors.size)
        assertEquals(10, AppearanceSwatchPalette.Columns)
        assertEquals(8, GemSwatchGridLayout.rows(AppearanceSwatchPalette.colors, AppearanceSwatchPalette.Columns).size)
        assertEquals("#FF0000", AppearanceSwatchPalette.colors.first().value)
        assertEquals("#EAD6EA", AppearanceSwatchPalette.colors.last().value)
    }
}
