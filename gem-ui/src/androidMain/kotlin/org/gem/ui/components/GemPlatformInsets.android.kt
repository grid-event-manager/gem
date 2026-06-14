package org.gem.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun GemPlatformInsets(modifier: Modifier): Modifier =
    modifier
        .safeDrawingPadding()
        .imePadding()
