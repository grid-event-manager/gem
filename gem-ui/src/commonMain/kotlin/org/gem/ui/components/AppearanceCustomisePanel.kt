package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.design.AppearanceTargetCatalogue
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun AppearanceCustomisePanel(
    state: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
    callbacks: AppearancePanelCallbacks,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.appearanceColorPickerGap),
    ) {
        GemColorPicker(
            selectedColor = state.activeColor,
            invalidRgbChannels = state.invalidRgbChannels,
            hexInputInvalid = state.hexInputInvalid,
            onSwatchSelected = callbacks.onColorSelected,
            onRgbValueChanged = callbacks.onRgbValueChanged,
            onRgbInputInvalid = callbacks.onRgbInputInvalid,
            onHexChanged = callbacks.onHexChanged,
        )
        AppearanceModeSelectors(
            state = state,
            textCatalogue = textCatalogue,
            callbacks = callbacks,
            enabled = enabled,
        )
        if (state.fontsVisible) {
            GemUnlabelledDropdownField(
                selectedLabel = state.activeFont.value,
                placeholderLabel = textCatalogue.text(GemTextKey.Fonts),
                options = AppearanceCustomisePanelInteraction.fontOptions(state, textCatalogue),
                onSelected = { selected -> selected?.let(callbacks.onFontSelected) },
                enabled = enabled,
            )
        }
        AppearanceActionRow(
            textCatalogue = textCatalogue,
            callbacks = callbacks,
            enabled = enabled,
        )
        GemTextPromptModal(
            visible = state.saveThemeDialogOpen,
            name = state.saveThemeName,
            placeholder = textCatalogue.text(GemTextKey.EnterNewThemeName),
            lightLabel = textCatalogue.text(GemTextKey.Light),
            darkLabel = textCatalogue.text(GemTextKey.Dark),
            checked = state.saveThemeMode == AppearanceMode.DARK,
            cancelText = textCatalogue.text(GemTextKey.Cancel),
            saveText = textCatalogue.text(GemTextKey.Save),
            onNameChange = callbacks.onSaveThemeNameChanged,
            onCheckedChange = { checked ->
                callbacks.onSaveThemeModeChanged(if (checked) AppearanceMode.DARK else AppearanceMode.LIGHT)
            },
            onCancel = callbacks.onCloseSaveThemeDialog,
            onSave = { callbacks.onSaveTheme(state.saveThemeName, state.saveThemeMode) },
        )
    }
}

@Composable
private fun AppearanceModeSelectors(
    state: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
    callbacks: AppearancePanelCallbacks,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.appearanceSelectorRowGap),
    ) {
        GemUnlabelledDropdownField(
            selectedLabel = AppearanceCustomisePanelInteraction.textTargetLabel(
                state.activeTextTarget,
                textCatalogue,
            ),
            placeholderLabel = textCatalogue.text(GemTextKey.Text),
            options = AppearanceCustomisePanelInteraction.textTargetOptions(textCatalogue),
            onOpen = { callbacks.onTextTargetSelected(state.activeTextTarget) },
            onSelected = { selected -> selected?.let(callbacks.onTextTargetSelected) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        GemUnlabelledDropdownField(
            selectedLabel = AppearanceCustomisePanelInteraction.elementTargetLabel(
                state.activeElementTarget,
                textCatalogue,
            ),
            placeholderLabel = textCatalogue.text(GemTextKey.Element),
            options = AppearanceCustomisePanelInteraction.elementTargetOptions(textCatalogue),
            onOpen = { callbacks.onElementTargetSelected(state.activeElementTarget) },
            onSelected = { selected -> selected?.let(callbacks.onElementTargetSelected) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppearanceActionRow(
    textCatalogue: GemTextCatalogue,
    callbacks: AppearancePanelCallbacks,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.appearanceProfileActionGap),
    ) {
        GemSecondaryButton(
            text = textCatalogue.text(GemTextKey.SaveTheme),
            onClick = callbacks.onOpenSaveThemeDialog,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        GemSecondaryButton(
            text = textCatalogue.text(GemTextKey.ResetToDefault),
            onClick = callbacks.onResetCurrentMode,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

internal object AppearanceCustomisePanelInteraction {
    val collapsedOrder: List<String> = listOf("color-picker", "target-row", "actions")
    val fontsVisibleOrder: List<String> = listOf("color-picker", "target-row", "fonts", "actions")

    fun layoutOrder(state: AppearanceUiState): List<String> =
        if (state.fontsVisible) fontsVisibleOrder else collapsedOrder

    fun textTargetOptions(textCatalogue: GemTextCatalogue): List<GemDropdownOption<AppearanceTextTarget>> =
        listOf<GemDropdownOption<AppearanceTextTarget>>(
            GemDropdownOption(
                null,
                textCatalogue.text(GemTextKey.Text),
                enabled = false,
            ),
        ) + AppearanceTargetCatalogue.textTargets.map { spec ->
            GemDropdownOption(
                spec.target,
                textCatalogue.text(spec.labelKey),
            )
        }

    fun elementTargetOptions(textCatalogue: GemTextCatalogue): List<GemDropdownOption<AppearanceElementTarget>> =
        listOf<GemDropdownOption<AppearanceElementTarget>>(
            GemDropdownOption(
                null,
                textCatalogue.text(GemTextKey.Element),
                enabled = false,
            ),
        ) + AppearanceTargetCatalogue.elementTargets.map { spec ->
            GemDropdownOption(
                spec.target,
                textCatalogue.text(spec.labelKey),
            )
        }

    fun fontOptions(
        state: AppearanceUiState,
        textCatalogue: GemTextCatalogue,
    ): List<GemDropdownOption<AppearanceFontFamily>> =
        listOf<GemDropdownOption<AppearanceFontFamily>>(
            GemDropdownOption(
                null,
                textCatalogue.text(GemTextKey.Fonts),
                enabled = false,
            ),
        ) + state.availableFontFamilies.map { family ->
            GemDropdownOption(
                family,
                family.value,
            )
        }

    fun textTargetLabel(
        target: AppearanceTextTarget,
        textCatalogue: GemTextCatalogue,
    ): String =
        textCatalogue.text(AppearanceTargetCatalogue.textTargets.first { it.target == target }.labelKey)

    fun elementTargetLabel(
        target: AppearanceElementTarget,
        textCatalogue: GemTextCatalogue,
    ): String =
        textCatalogue.text(AppearanceTargetCatalogue.elementTargets.first { it.target == target }.labelKey)
}
