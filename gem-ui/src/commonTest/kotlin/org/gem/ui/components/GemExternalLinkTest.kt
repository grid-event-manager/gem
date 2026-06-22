package org.gem.ui.components

import androidx.compose.ui.text.style.TextDecoration
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class GemExternalLinkTest {
    @Test
    fun externalLinkUsesSharedButtonTextAndInkTokens() {
        val typeScale = GemTypeScale()
        val colors = GemColors()

        assertEquals(typeScale.button.fontSize, GemExternalLinkTokens.textStyle(typeScale).fontSize)
        assertEquals(TextDecoration.Underline, GemExternalLinkTokens.textStyle(typeScale).textDecoration)
        assertEquals(colors.buttonLabelInk, GemExternalLinkTokens.textColor(colors))
    }
}
