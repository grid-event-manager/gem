package org.hostess.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HostessSpacing(
    val none: Dp = 0.dp,
    val borderWidth: Dp = 1.dp,
    val tapTarget: Dp = 48.dp,
    val pagePadding: Dp = 14.dp,
    val shellMaxWidth: Dp = 412.dp,
    val shellMaxHeight: Dp = 932.dp,
    val shellMinHeight: Dp = 760.dp,
    val desktopWindowWidth: Dp = 440.dp,
    val desktopWindowHeight: Dp = 780.dp,
    val topBarMinHeight: Dp = 72.dp,
    val sessionStripMinHeight: Dp = 62.dp,
    val panelPadding: Dp = 16.dp,
    val fieldGap: Dp = 8.dp,
    val rowGap: Dp = 10.dp,
    val controlHeight: Dp = 56.dp,
    val fieldHorizontalPadding: Dp = 12.dp,
    val rowVerticalPadding: Dp = 9.dp,
    val rowHorizontalPadding: Dp = 12.dp,
    val rowIconSize: Dp = 42.dp,
    val statusPillMinHeight: Dp = 28.dp,
    val statusPillHorizontalPadding: Dp = 10.dp,
    val noticeBodyMinHeight: Dp = 112.dp,
    val noticeBodyMaxHeight: Dp = 180.dp,
    val inventoryPaneMaxHeight: Dp = 220.dp,
    val groupPickerMaxHeight: Dp = 220.dp,
    val scrollListMaxHeight: Dp = 220.dp,
    val modalInset: Dp = 22.dp,
    val modalMaxWidth: Dp = 360.dp,
    val modalActionMinWidth: Dp = 112.dp,
    val inlineGap: Dp = 12.dp,
)
