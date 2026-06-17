package org.gem.ui.design

import org.gem.core.appearance.AppearanceMode

interface GemPaletteProvider {
    fun colors(mode: AppearanceMode): GemColors
}
