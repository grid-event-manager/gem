package org.gem.ui.state

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileWarning
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.core.theme.ThemePreference
import org.gem.ui.text.GemTextKey

data class AppearanceUiState(
    val mode: AppearanceMode,
    val themePreference: ThemePreference,
    val selectedProfileId: AppearanceProfileId?,
    val stockProfiles: List<AppearanceProfile>,
    val customProfiles: List<AppearanceProfile>,
    val availableFontFamilies: List<AppearanceFontFamily>,
    val currentDraft: AppearanceDraft,
    val expandedPanel: AppearanceExpandedPanel = AppearanceExpandedPanel.NONE,
    val activeEditMode: AppearanceEditMode = AppearanceEditMode.TEXT,
    val activeTextTarget: AppearanceTextTarget = AppearanceTextTarget.TITLE_BAR,
    val activeElementTarget: AppearanceElementTarget = AppearanceElementTarget.PAGE_BACKGROUND,
    val textTargetSelectorHasConcreteSelection: Boolean = false,
    val elementTargetSelectorHasConcreteSelection: Boolean = false,
    val fontsVisible: Boolean = false,
    val invalidRgbChannels: Set<AppearanceRgbChannel> = emptySet(),
    val hexInputInvalid: Boolean = false,
    val saveThemeDialogOpen: Boolean = false,
    val saveThemeMode: AppearanceMode = mode,
    val saveThemeName: String = "",
    val saveThemeNameFocusRequested: Boolean = false,
    val profileWarning: AppearanceProfileWarning? = null,
    val errorKey: GemTextKey? = null,
) {
    val profiles: List<AppearanceProfile>
        get() = stockProfiles + customProfiles

    val activeColor: AppearanceColor
        get() = when (activeEditMode) {
            AppearanceEditMode.TEXT -> currentDraft.textColors.getValue(activeTextTarget)
            AppearanceEditMode.ELEMENT -> currentDraft.elementColors.getValue(activeElementTarget)
        }

    val activeFont: AppearanceFontFamily
        get() = currentDraft.textFonts.getValue(activeTextTarget)

    val toggleChecked: Boolean
        get() = mode == AppearanceMode.DARK

    companion object {
        fun loading(osDark: Boolean): AppearanceUiState {
            val mode = if (osDark) AppearanceMode.DARK else AppearanceMode.LIGHT
            return AppearanceUiState(
                mode = mode,
                themePreference = ThemePreference.SYSTEM,
                selectedProfileId = null,
                stockProfiles = emptyList(),
                customProfiles = emptyList(),
                availableFontFamilies = emptyList(),
                currentDraft = AppearanceDraft.loadingPlaceholder(mode),
            )
        }
    }
}

enum class AppearanceExpandedPanel {
    NONE,
    CUSTOMISE,
    THEMES,
}

enum class AppearanceEditMode {
    TEXT,
    ELEMENT,
}

enum class AppearanceRgbChannel {
    R,
    G,
    B,
}
