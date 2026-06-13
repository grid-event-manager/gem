package org.gem.apps.desktop

internal object GemDesktopRenderPolicy {
    private const val SkikoRenderApiProperty = "skiko.renderApi"
    private const val WindowsSoftwareRenderer = "SOFTWARE"

    fun install() {
        install(
            osName = System.getProperty("os.name").orEmpty(),
            readProperty = System::getProperty,
            writeProperty = System::setProperty,
        )
    }

    internal fun install(
        osName: String,
        readProperty: (String) -> String?,
        writeProperty: (String, String) -> String?,
    ) {
        if (!osName.startsWith("Windows", ignoreCase = true)) {
            return
        }
        if (!readProperty(SkikoRenderApiProperty).isNullOrBlank()) {
            return
        }
        writeProperty(SkikoRenderApiProperty, WindowsSoftwareRenderer)
    }
}
