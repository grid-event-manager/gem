package org.hostess.ui.design

interface HostessPaletteProvider {
    fun colors(mode: ResolvedThemeMode): HostessColors
}
