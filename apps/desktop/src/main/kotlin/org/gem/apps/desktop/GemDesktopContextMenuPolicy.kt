package org.gem.apps.desktop

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
internal object GemDesktopContextMenuPolicy {
    fun install() {
        ComposeFoundationFlags.isNewContextMenuEnabled = false
    }
}
