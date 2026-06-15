package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GemDesignTokenTest {
    @Test
    fun colorsMatchPrototypeCatalogue() {
        val colors = GemColors()

        assertEquals(Color(0xFFDCE3EC), colors.page)
        assertEquals(Color(0xFFF8FAFC), colors.surface)
        assertEquals(Color(0xFFFFFFFF), colors.surfaceStrong)
        assertEquals(Color(0xFF151923), colors.ink)
        assertEquals(Color(0xFF5D6675), colors.muted)
        assertEquals(Color(0xFFD5DBE5), colors.line)
        assertEquals(Color(0xFFBAC4D1), colors.lineStrong)
        assertEquals(Color(0xFF0F766E), colors.primary)
        assertEquals(Color(0xFFFFFFFF), colors.primaryInk)
        assertEquals(Color(0xFF315A9A), colors.secondary)
        assertEquals(Color(0xFFA32936), colors.danger)
        assertEquals(Color(0xFFDFF7EE), colors.successBackground)
        assertEquals(Color(0xFF116049), colors.successInk)
        assertEquals(Color(0xFFE8F5F3), colors.selectedBackground)
        assertEquals(Color(0xFF18202B), colors.topBar)
        assertEquals(Color(0xFFF7C948), colors.brandMark)
    }

    @Test
    fun spacingAndShapesMatchPrototypeCatalogue() {
        val spacing = GemSpacing()
        val shapes = GemShapes()

        assertEquals(48.dp, spacing.tapTarget)
        assertEquals(14.dp, spacing.pagePadding)
        assertEquals(412.dp, spacing.shellMaxWidth)
        assertEquals(932.dp, spacing.shellMaxHeight)
        assertEquals(760.dp, spacing.shellMinHeight)
        assertEquals(440.dp, spacing.desktopWindowWidth)
        assertEquals(780.dp, spacing.desktopWindowHeight)
        assertEquals(72.dp, spacing.topBarMinHeight)
        assertEquals(72.dp, spacing.secondLifeTimeMinWidth)
        assertEquals(8.dp, spacing.secondLifeTimeMenuGap)
        assertEquals(62.dp, spacing.sessionStripMinHeight)
        assertEquals(16.dp, spacing.panelPadding)
        assertEquals(14.dp, spacing.operationSpinnerSize)
        assertEquals(220.dp, spacing.scrollListMaxHeight)
        assertEquals(112.dp, spacing.modalActionMinWidth)
        assertEquals(112.dp, spacing.noticeBodyMinHeight)
        assertEquals(180.dp, spacing.noticeBodyMaxHeight)
        assertEquals(220.dp, spacing.inventoryPaneMaxHeight)
        assertEquals(220.dp, spacing.groupPickerMaxHeight)
        assertEquals(8.dp, spacing.fieldGap)
        assertEquals(10.dp, spacing.rowGap)
        assertEquals(8.dp, shapes.panelRadius)
        assertEquals(8.dp, shapes.controlRadius)
    }

    @Test
    fun typeScaleUsesFixedPrototypeSizes() {
        val typeScale = GemTypeScale()

        assertEquals(20.sp, typeScale.brandTitle.fontSize)
        assertEquals(10.sp, typeScale.brandSubtitle.fontSize)
        assertEquals(16.sp, typeScale.sectionTitle.fontSize)
        assertEquals(14.sp, typeScale.body.fontSize)
        assertEquals(12.sp, typeScale.smallLabel.fontSize)
        assertEquals(14.sp, typeScale.button.fontSize)
        assertEquals(12.sp, typeScale.statusPill.fontSize)
        assertEquals(FontWeight.Bold, typeScale.brandTitle.fontWeight)
        assertEquals(FontWeight.Normal, typeScale.brandSubtitle.fontWeight)
        assertNull(typeScale.sectionTitle.fontWeight)
        assertNull(typeScale.button.fontWeight)
    }
}
