package org.gem.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.gem.ui.design.GemTheme

@Composable
fun SectionBackNav(
    text: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val backInteraction = remember { MutableInteractionSource() }
    val backHovered by backInteraction.collectIsHoveredAsState()
    val backColor = if (backHovered) {
        GemTheme.colors.interactiveHoverInk
    } else {
        GemTheme.colors.navigationInk
    }
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        val testTagModifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .hoverable(backInteraction)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onBack,
                    )
                    .then(testTagModifier),
                horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemBackIcon(tint = backColor)
                Text(
                    text = text,
                    color = backColor,
                    style = GemTheme.typeScale.smallLabel,
                )
            }
        }
        HorizontalDivider(
            color = GemTheme.colors.line,
            thickness = GemTheme.spacing.borderWidth,
        )
    }
}
