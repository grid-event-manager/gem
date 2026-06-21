package org.gem.apps.desktop

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPlatformLocaleProviderTest {
    @Test
    fun reportsJvmDefaultLocaleTagFromInjectedLocaleSource() {
        val provider = DesktopPlatformLocaleProvider { Locale.UK }

        assertEquals("en-GB", provider.currentLocaleTag())
    }
}
