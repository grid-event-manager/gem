package org.gem.ui.screens

import androidx.compose.ui.unit.dp
import org.gem.ui.design.GemSpacing
import org.gem.ui.testtags.GemTestTags
import kotlin.test.Test
import kotlin.test.assertEquals

class GemAppScaffoldTest {
    @Test
    fun `scaffold contract uses shared phone form tokens and app tag`() {
        val spacing = GemSpacing()

        assertEquals("data-gem-app", GemTestTags.GemApp)
        assertEquals(412.dp, spacing.shellMaxWidth)
        assertEquals(14.dp, spacing.pagePadding)
        assertEquals(10.dp, spacing.rowGap)
    }
}
