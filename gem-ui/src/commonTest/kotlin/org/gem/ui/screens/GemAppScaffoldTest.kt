package org.gem.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.gem.ui.components.GemAppScaffold
import org.gem.ui.design.GemSpacing
import org.gem.ui.testtags.GemTestTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GemAppScaffoldTest {
    @Test
    fun `scaffold contract uses shared phone form tokens and app tag`() {
        val spacing = GemSpacing()

        assertEquals("data-gem-app", GemTestTags.GemApp)
        assertEquals(412.dp, spacing.shellMaxWidth)
        assertEquals(14.dp, spacing.pagePadding)
        assertEquals(10.dp, spacing.rowGap)
    }

    @Test
    fun `scaffold contract accepts stable named slots`() {
        val scaffold: @Composable () -> Unit = {
            GemAppScaffold(
                topBar = {},
                navigation = {},
                sessionStrip = {},
                content = {},
                footer = {},
            )
        }

        assertNotNull(scaffold)
    }
}
