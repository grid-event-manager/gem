package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTheme
import org.gem.ui.design.GemTypeScale

enum class GemDropdownOptionVisualTone {
    DEFAULT,
    PLACEHOLDER,
    DISABLED,
}

data class GemDropdownOption<T>(
    val value: T?,
    val label: String,
    val enabled: Boolean = true,
    val visualTone: GemDropdownOptionVisualTone = if (enabled) {
        GemDropdownOptionVisualTone.DEFAULT
    } else {
        GemDropdownOptionVisualTone.DISABLED
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GemDropdownField(
    label: String,
    selectedLabel: String?,
    placeholderLabel: String,
    options: List<GemDropdownOption<T>>,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldModifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val spacing = GemTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
    ) {
        GemFieldLabel(label)
        GemDropdownSelector(
            selectedLabel = selectedLabel,
            placeholderLabel = placeholderLabel,
            expanded = expanded,
            onOpen = { expanded = true },
            enabled = enabled,
            modifier = fieldModifier,
        )
        GemDropdownMenu(
            expanded = expanded,
            options = options,
            onDismiss = { expanded = false },
            onSelected = {
                expanded = false
                onSelected(it)
            },
        )
    }
}

@Composable
fun <T> GemUnlabelledDropdownField(
    selectedLabel: String?,
    placeholderLabel: String,
    options: List<GemDropdownOption<T>>,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldModifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
    textAlign: TextAlign = TextAlign.Start,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
    ) {
        GemDropdownSelector(
            selectedLabel = selectedLabel,
            placeholderLabel = placeholderLabel,
            expanded = expanded,
            onOpen = {
                onOpen()
                expanded = true
            },
            enabled = enabled,
            modifier = fieldModifier,
            textAlign = textAlign,
        )
        GemDropdownMenu(
            expanded = expanded,
            options = options,
            onDismiss = { expanded = false },
            onSelected = {
                expanded = false
                onSelected(it)
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GemDropdownSelector(
    selectedLabel: String?,
    placeholderLabel: String,
    expanded: Boolean,
    onOpen: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.controlHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onOpen)
            .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true),
        shape = GemTheme.shapes.control,
        color = colors.fieldSurface,
        contentColor = if (enabled) colors.ink else colors.disabledInk,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.controlHeight)
                .padding(horizontal = spacing.fieldHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.fieldGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (GemDropdownTokens.needsCenteredSelectorLeadingSlot(textAlign)) {
                Spacer(modifier = Modifier.width(spacing.dropdownTrailingIconSlotWidth))
            }
            Text(
                text = selectedLabel ?: placeholderLabel,
                style = GemDropdownTokens.selectorTextStyle(GemTheme.typeScale),
                color = GemDropdownTokens.selectorTextColor(colors, selectedLabel != null),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GemDropdownTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholderLabel: String,
    options: List<GemDropdownOption<T>>,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
    ) {
        GemFieldLabel(label)
        ExposedDropdownMenuBox(
            expanded = expanded && options.isNotEmpty(),
            onExpandedChange = {
                if (enabled && options.isNotEmpty()) {
                    expanded = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = GemDropdownTokens.selectorTextStyle(GemTheme.typeScale),
                shape = GemTheme.shapes.control,
                colors = gemTextFieldColors(),
                trailingIcon = {
                    if (options.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = fieldModifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
                    .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)
                    .fillMaxWidth()
                    .height(GemTheme.spacing.controlHeight),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = GemTheme.colors.menuSurface,
                modifier = Modifier.background(GemTheme.colors.menuSurface),
            ) {
                GemDropdownMenuItems(
                    options = options,
                    onSelected = {
                        expanded = false
                        onSelected(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> GemDropdownMenu(
    expanded: Boolean,
    options: List<GemDropdownOption<T>>,
    onDismiss: () -> Unit,
    onSelected: (T?) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = GemTheme.colors.menuSurface,
        modifier = Modifier.background(GemTheme.colors.menuSurface),
    ) {
        GemDropdownMenuItems(
            options = options,
            onSelected = onSelected,
        )
    }
}

@Composable
private fun <T> GemDropdownMenuItems(
    options: List<GemDropdownOption<T>>,
    onSelected: (T?) -> Unit,
) {
    options.forEach { option ->
        GemDropdownMenuItem(
            label = option.label,
            enabled = option.enabled,
            visualTone = option.visualTone,
            onClick = { onSelected(option.value) },
        )
    }
}

@Composable
private fun GemDropdownMenuItem(
    label: String,
    enabled: Boolean,
    visualTone: GemDropdownOptionVisualTone,
    onClick: () -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = GemMenuTextTokens.textStyle(GemTheme.typeScale),
                color = GemDropdownTokens.menuTextColor(colors, visualTone),
            )
        },
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = spacing.menuItemHorizontalPadding,
            vertical = spacing.menuItemVerticalPadding,
        ),
        colors = MenuDefaults.itemColors(
            textColor = GemDropdownTokens.menuTextColor(colors, visualTone),
            disabledTextColor = GemDropdownTokens.menuTextColor(colors, GemDropdownOptionVisualTone.DISABLED),
        ),
    )
}

internal object GemDropdownTokens {
    fun needsCenteredSelectorLeadingSlot(textAlign: TextAlign): Boolean =
        textAlign == TextAlign.Center

    fun selectorTextColor(
        colors: GemColors,
        hasSelection: Boolean,
    ): Color =
        if (hasSelection) colors.ink else colors.muted

    fun selectorTextStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.fieldText

    fun menuTextColor(
        colors: GemColors,
        visualTone: GemDropdownOptionVisualTone,
    ): Color =
        when (visualTone) {
            GemDropdownOptionVisualTone.DEFAULT -> GemMenuTextTokens.textColor(colors, enabled = true)
            GemDropdownOptionVisualTone.PLACEHOLDER,
            GemDropdownOptionVisualTone.DISABLED,
            -> GemMenuTextTokens.textColor(colors, enabled = false)
        }
}
