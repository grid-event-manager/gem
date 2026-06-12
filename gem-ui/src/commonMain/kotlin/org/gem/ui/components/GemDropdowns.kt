package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import org.gem.ui.design.GemTheme

data class GemDropdownOption<T>(
    val value: T?,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HostessDropdownField(
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
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
    ) {
        HostessFieldLabel(label)
        Surface(
            modifier = fieldModifier
                .fillMaxWidth()
                .height(spacing.controlHeight)
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
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
                Text(
                    text = selectedLabel ?: placeholderLabel,
                    style = GemTheme.typeScale.body,
                    color = if (selectedLabel == null) colors.muted else colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        HostessDropdownMenu(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HostessDropdownTextField(
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
        HostessFieldLabel(label)
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
                textStyle = GemTheme.typeScale.body,
                shape = GemTheme.shapes.control,
                colors = hostessTextFieldColors(),
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
                HostessDropdownMenuItems(
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
private fun <T> HostessDropdownMenu(
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
        HostessDropdownMenuItems(
            options = options,
            onSelected = onSelected,
        )
    }
}

@Composable
private fun <T> HostessDropdownMenuItems(
    options: List<GemDropdownOption<T>>,
    onSelected: (T?) -> Unit,
) {
    options.forEach { option ->
        HostessDropdownMenuItem(label = option.label, onClick = { onSelected(option.value) })
    }
}

@Composable
private fun HostessDropdownMenuItem(
    label: String,
    onClick: () -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = GemTheme.typeScale.body,
                color = colors.secondary,
            )
        },
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = spacing.menuItemHorizontalPadding,
            vertical = spacing.menuItemVerticalPadding,
        ),
        colors = MenuDefaults.itemColors(textColor = colors.secondary),
    )
}
