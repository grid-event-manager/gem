package org.gem.apps.desktop

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalFoundationApi::class)
class GemDesktopContextMenuPolicyTest {
    @Test
    fun `desktop startup disables hardcoded compose text context menu path`() {
        val previous = ComposeFoundationFlags.isNewContextMenuEnabled
        try {
            ComposeFoundationFlags.isNewContextMenuEnabled = true

            GemDesktopContextMenuPolicy.install()

            assertFalse(ComposeFoundationFlags.isNewContextMenuEnabled)
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previous
        }
    }
}
