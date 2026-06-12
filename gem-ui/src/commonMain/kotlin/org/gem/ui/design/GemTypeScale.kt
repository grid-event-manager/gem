package org.gem.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class GemTypeScale(
    val brandTitle: TextStyle = TextStyle(
        fontSize = 20.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
    ),
    val sectionTitle: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    val body: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    val smallLabel: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
    ),
    val button: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val menuItem: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val statusPill: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
    ),
)
