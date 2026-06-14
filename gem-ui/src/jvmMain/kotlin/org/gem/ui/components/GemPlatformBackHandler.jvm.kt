package org.gem.ui.components

import androidx.compose.runtime.Composable

@Composable
internal actual fun GemPlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
