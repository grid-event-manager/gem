package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceTextTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class GemSystemThemeProfileFactoryTest {
    @Test
    fun `factory calls provider once and fills every text target`() {
        var calls = 0
        val profile = GemSystemThemeProfileFactory.profile(
            mode = AppearanceMode.LIGHT,
            availableFontFamilies = listOf(AppearanceFontFamily("Noto Sans")),
            platformSystemFontFamilyProvider = PlatformSystemFontFamilyProvider {
                calls += 1
                AppearanceFontFamily("Noto Sans")
            },
        )

        assertEquals(1, calls)
        assertEquals(AppearanceProfileSource.SYSTEM, profile.source)
        assertEquals(AppearanceTextTarget.entries.toSet(), profile.textFonts.keys)
        AppearanceTextTarget.entries.forEach { target ->
            assertEquals(AppearanceFontFamily("Noto Sans"), profile.textFonts.getValue(target))
        }
    }
}
