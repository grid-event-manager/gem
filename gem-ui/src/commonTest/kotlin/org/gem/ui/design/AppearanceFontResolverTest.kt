package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceFontResolverTest {
    @Test
    fun resolvesExactPlatformMatchCaseInsensitively() {
        val resolver = AppearanceFontResolver(
            listOf(
                AppearanceFontFamily("Inter"),
                AppearanceFontFamily("JetBrains Mono"),
            ),
        )

        assertEquals(
            AppearanceFontFamily("Inter"),
            resolver.resolve(
                requested = AppearanceFontFamily("inter"),
                targetDefault = AppearanceFontFamily("sans-serif"),
            ),
        )
    }

    @Test
    fun resolvesTargetDefaultBeforeFirstPlatformFamily() {
        val resolver = AppearanceFontResolver(
            listOf(
                AppearanceFontFamily("System Default"),
                AppearanceFontFamily("Inter"),
            ),
        )

        assertEquals(
            AppearanceFontFamily("Inter"),
            resolver.resolve(
                requested = AppearanceFontFamily("Missing"),
                targetDefault = AppearanceFontFamily("inter"),
            ),
        )
    }

    @Test
    fun resolvesFirstPlatformFamilyWhenRequestedAndDefaultAreMissing() {
        val resolver = AppearanceFontResolver(listOf(AppearanceFontFamily("System Default")))

        assertEquals(
            AppearanceFontFamily("System Default"),
            resolver.resolve(
                requested = AppearanceFontFamily("Missing"),
                targetDefault = AppearanceFontFamily("Also Missing"),
            ),
        )
    }

    @Test
    fun resolvesLiteralSansSerifWhenPlatformCatalogueIsBlank() {
        val resolver = AppearanceFontResolver(emptyList())

        assertEquals(
            AppearanceFontFamily("sans-serif"),
            resolver.resolve(
                requested = AppearanceFontFamily("Missing"),
                targetDefault = AppearanceFontFamily("Also Missing"),
            ),
        )
    }
}
