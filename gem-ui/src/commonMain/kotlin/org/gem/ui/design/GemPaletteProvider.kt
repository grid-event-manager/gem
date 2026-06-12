package org.gem.ui.design

interface GemPaletteProvider {
    fun colors(mode: ResolvedThemeMode): GemColors
}
