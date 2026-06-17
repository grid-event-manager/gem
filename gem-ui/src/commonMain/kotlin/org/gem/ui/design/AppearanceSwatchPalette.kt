package org.gem.ui.design

import org.gem.core.appearance.AppearanceColor

object AppearanceSwatchPalette {
    const val Columns: Int = 10

    val colors: List<AppearanceColor> = listOf(
        "#ff0000", "#7a0000", "#000000", "#441111", "#7b1212", "#ff3333", "#ff6666", "#ff9b87", "#ffc9bd", "#f3e6dc",
        "#ff7a00", "#8a3b00", "#3d1d00", "#653800", "#9b5f00", "#ff9900", "#ffbd54", "#ffd68b", "#ffe8bf", "#f2dcc6",
        "#ffff00", "#878700", "#303000", "#5a5a00", "#8a8a00", "#c8c800", "#eeee33", "#ffff73", "#ffffb8", "#f2f0d4",
        "#00ff00", "#008500", "#003200", "#005600", "#008a2a", "#00b84a", "#28d86b", "#72f08f", "#baf5c7", "#d5ead8",
        "#00ffff", "#008686", "#002f2f", "#005c66", "#008ca0", "#00b8d6", "#35d7f0", "#83eafa", "#c5f5fa", "#d9edf0",
        "#0066ff", "#003a8a", "#001b3f", "#0a356c", "#1c5aa8", "#3984ff", "#73a9ff", "#a9caff", "#d3e2ff", "#d5dde9",
        "#0000ff", "#00008a", "#00003f", "#1f1f70", "#37379f", "#5b5bff", "#8b8bff", "#b8b8ff", "#ddddff", "#d6d6e8",
        "#ff00ff", "#8b008b", "#3a003a", "#650065", "#9b249b", "#d335d3", "#f06cf0", "#ff9dff", "#ffcfff", "#ead6ea",
    ).map(AppearanceColor::require)
}
