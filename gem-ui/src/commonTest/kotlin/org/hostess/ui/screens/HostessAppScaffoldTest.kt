package org.hostess.ui.screens

import androidx.compose.ui.unit.dp
import org.hostess.ui.design.HostessSpacing
import org.hostess.ui.testtags.HostessTestTags
import kotlin.test.Test
import kotlin.test.assertEquals

class HostessAppScaffoldTest {
    @Test
    fun `scaffold contract uses shared phone form tokens and app tag`() {
        val spacing = HostessSpacing()

        assertEquals("data-hostess-app", HostessTestTags.HostessApp)
        assertEquals(412.dp, spacing.shellMaxWidth)
        assertEquals(14.dp, spacing.pagePadding)
        assertEquals(10.dp, spacing.rowGap)
    }
}
