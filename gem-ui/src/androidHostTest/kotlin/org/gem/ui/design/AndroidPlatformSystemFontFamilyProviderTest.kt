package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidPlatformSystemFontFamilyProviderTest {
    @Test
    fun `android provider selects sans serif`() {
        assertEquals(
            AppearanceFontFamily("sans-serif"),
            AndroidPlatformSystemFontFamilyProvider.defaultFamily(
                listOf(AppearanceFontFamily("Roboto"), AppearanceFontFamily("Sans-Serif")),
            ),
        )
    }
}
