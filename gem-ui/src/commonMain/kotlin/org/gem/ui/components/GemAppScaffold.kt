package org.gem.ui.components

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
import org.gem.ui.design.GemTheme
import org.gem.ui.testtags.GemTestTags

@Composable
fun GemAppScaffold(
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
            .background(GemTheme.colors.page)
            .testTag(GemTestTags.GemApp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = GemTheme.spacing.shellMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(GemTheme.spacing.pagePadding),
            verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
        ) {
            topBar()
            navigation?.invoke()
            sessionStrip()
            GemScrollableColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
                content = content,
            )
            footer?.invoke()
        }
    }
}
