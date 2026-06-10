package org.hostess.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HostessMenuIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun HostessBackIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = null,
        modifier = modifier,
    )
}
