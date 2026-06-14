package org.gem.ui.components

import androidx.compose.runtime.Composable

@Composable
internal expect fun GemPlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
