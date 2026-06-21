package org.gem.apps.desktop

import java.util.Locale
import org.gem.ui.text.PlatformLocaleProvider

class DesktopPlatformLocaleProvider(
    private val currentLocale: () -> Locale = Locale::getDefault,
) : PlatformLocaleProvider {
    override fun currentLocaleTag(): String =
        currentLocale().toLanguageTag()
}
