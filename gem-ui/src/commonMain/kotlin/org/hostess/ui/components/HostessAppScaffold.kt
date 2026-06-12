package org.hostess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags

@Composable
fun HostessAppScaffold(
    topBar: @Composable () -> Unit,
    sessionStrip: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HostessTheme.colors.page)
            .testTag(HostessTestTags.HostessApp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = HostessTheme.spacing.shellMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(HostessTheme.spacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
        ) {
            topBar()
            navigation?.invoke()
            sessionStrip()
            HostessScrollableColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
                content = content,
            )
            footer?.invoke()
        }
    }
}
