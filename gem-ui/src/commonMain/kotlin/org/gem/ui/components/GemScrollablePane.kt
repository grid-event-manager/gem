package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import org.gem.ui.design.GemTheme

@Composable
fun GemScrollablePane(
    minHeight: Dp,
    maxHeight: Dp,
    testTag: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(GemTheme.spacing.fieldGap),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.fieldSurface,
        contentColor = colors.ink,
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight, max = maxHeight)
                    .verticalScroll(scrollState)
                    .padding(
                        start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateRightPadding(LayoutDirection.Ltr) +
                            GemPlatformUi.scrollbarContentInset,
                        bottom = contentPadding.calculateBottomPadding(),
                    )
                    .testTag(testTag),
                verticalArrangement = verticalArrangement,
                content = content,
            )
            HostessPlatformVerticalScrollbar(
                scrollState = scrollState,
                thumbColor = colors.secondary.copy(alpha = ScrollbarThumbAlpha),
                hoverThumbColor = colors.secondary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(spacing.scrollbarWidth),
            )
        }
    }
}

@Composable
fun HostessScrollableColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(end = GemTheme.spacing.none),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        HostessPlatformVerticalScrollbar(
            scrollState = scrollState,
            thumbColor = colors.secondary.copy(alpha = ScrollbarThumbAlpha),
            hoverThumbColor = colors.secondary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(spacing.scrollbarWidth),
        )
    }
}

private const val ScrollbarThumbAlpha = 0.7f

object GemPlatformUi {
    val scrollbarContentInset: Dp
        @Composable
        get() = if (HostessPlatformVisibleScrollbars) {
            GemTheme.spacing.scrollbarWidth
        } else {
            GemTheme.spacing.none
        }
}

expect val HostessPlatformVisibleScrollbars: Boolean

@Composable
internal expect fun HostessPlatformVerticalScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    hoverThumbColor: Color,
    modifier: Modifier = Modifier,
)
