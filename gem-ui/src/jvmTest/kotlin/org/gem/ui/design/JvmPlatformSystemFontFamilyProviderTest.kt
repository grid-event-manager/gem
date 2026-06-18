package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmPlatformSystemFontFamilyProviderTest {
    @Test
    fun `linux chain selects noto sans`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "Linux")

        assertEquals(
            AppearanceFontFamily("Noto Sans"),
            provider.defaultFamily(available("Arial", "Noto Sans")),
        )
    }

    @Test
    fun `windows chain selects segoe ui`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "Windows 11")

        assertEquals(
            AppearanceFontFamily("Segoe UI"),
            provider.defaultFamily(available("Arial", "Segoe UI")),
        )
    }

    @Test
    fun `mac chain selects helvetica neue`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "Mac OS X")

        assertEquals(
            AppearanceFontFamily("Helvetica Neue"),
            provider.defaultFamily(available("Helvetica", "Helvetica Neue")),
        )
    }

    @Test
    fun `darwin chain selects helvetica neue`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "Darwin")

        assertEquals(
            AppearanceFontFamily("Helvetica Neue"),
            provider.defaultFamily(available("Helvetica Neue", "Segoe UI")),
        )
    }

    @Test
    fun `candidate matching is case insensitive and returns normalized instance`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "linux")

        assertEquals(
            AppearanceFontFamily("noto sans"),
            provider.defaultFamily(available("noto sans")),
        )
    }

    @Test
    fun `chain exhaustion returns sans serif`() {
        val provider = JvmPlatformSystemFontFamilyProvider(osName = "linux")

        assertEquals(
            AppearanceFontFamily("sans-serif"),
            provider.defaultFamily(available("Courier")),
        )
    }

    private fun available(vararg values: String): List<AppearanceFontFamily> =
        values.map { AppearanceFontFamily(it) }
}
