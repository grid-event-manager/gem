package org.gem.ui.controllers

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceColorParseResult
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileLoadResult
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileResetResult
import org.gem.core.appearance.AppearanceProfileSaveResult
import org.gem.core.appearance.AppearanceProfileSelectionResult
import org.gem.core.appearance.AppearanceProfileState
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.ui.design.AppearanceFontResolver
import org.gem.ui.design.AppearanceTargetCatalogue
import org.gem.ui.design.GemSystemThemeProfileFactory
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.AppearanceEditMode
import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.state.AppearanceRgbChannel
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.text.GemTextKey

class AppearanceController(
    private val runtime: GemUiRuntime,
    val state: AppearanceUiState,
) {
    fun refresh(osDark: Boolean): AppearanceController {
        val themeSnapshot = runtime.themePreferenceService.loadPreference()
        val mode = resolve(themeSnapshot.preference, osDark)
        val loaded = runtime.appearanceProfileService.loadState()
        val profileState = loaded.state()
        val errorKey = when {
            themeSnapshot.warning != null -> GemTextKey.ThemePreferenceUnavailable
            loaded is AppearanceProfileLoadResult.StorageFailed -> GemTextKey.ThemePreferenceUnavailable
            else -> null
        }

        return copy(
            state = stateFrom(
                mode = mode,
                themePreference = themeSnapshot.preference,
                profileState = profileState,
                availableFontFamilies = runtime.platformFontCatalogue.availableFamilies(),
                errorKey = errorKey,
            ),
        )
    }

    fun setManualTheme(
        mode: AppearanceMode,
        osDark: Boolean,
    ): AppearanceController {
        val preference = mode.toPreference()
        return when (runtime.themePreferenceService.savePreference(preference)) {
            ThemePreferenceSaveResult.Saved -> {
                when (val reset = runtime.appearanceProfileService.resetMode(mode)) {
                    is AppearanceProfileResetResult.Reset -> copy(
                        state = stateFrom(
                            mode = resolve(preference, osDark),
                            themePreference = preference,
                            profileState = reset.state,
                            availableFontFamilies = state.availableFontFamilies,
                            errorKey = null,
                        ),
                    )
                    is AppearanceProfileResetResult.StorageFailed -> copy(
                        state = state.copy(
                            mode = mode,
                            themePreference = preference,
                            selectedProfileId = null,
                            currentDraft = systemDraft(mode, state.availableFontFamilies),
                            errorKey = GemTextKey.ThemePreferenceSaveFailed,
                        ),
                    )
                }
            }
            is ThemePreferenceSaveResult.StorageFailed -> copy(
                state = state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
            )
        }
    }

    fun selectProfile(profileId: AppearanceProfileId): AppearanceController =
        when (val selected = runtime.appearanceProfileService.selectProfile(profileId)) {
            is AppearanceProfileSelectionResult.Selected -> {
                val preference = selected.profile.mode.toPreference()
                val errorKey = when (runtime.themePreferenceService.savePreference(preference)) {
                    ThemePreferenceSaveResult.Saved -> null
                    is ThemePreferenceSaveResult.StorageFailed -> GemTextKey.ThemePreferenceSaveFailed
                }
                copy(
                    state = stateFrom(
                        mode = selected.profile.mode,
                        themePreference = preference,
                        profileState = selected.state,
                        availableFontFamilies = state.availableFontFamilies,
                        currentDraft = AppearanceDraft.fromProfile(selected.profile),
                        errorKey = errorKey,
                    ).copy(
                        saveThemeDialogOpen = false,
                        saveThemeName = "",
                        saveThemeNameFocusRequested = false,
                    ),
                )
            }
            is AppearanceProfileSelectionResult.ProfileMissing,
            is AppearanceProfileSelectionResult.StorageFailed,
            -> copy(state = state.copy(errorKey = GemTextKey.ThemePreferenceUnavailable))
        }

    fun updateTextTarget(target: AppearanceTextTarget): AppearanceController =
        copy(
            state = state.copy(
                activeEditMode = AppearanceEditMode.TEXT,
                activeTextTarget = target,
                fontsVisible = true,
                invalidRgbChannels = emptySet(),
                hexInputInvalid = false,
            ),
        )

    fun updateElementTarget(target: AppearanceElementTarget): AppearanceController =
        copy(
            state = state.copy(
                activeEditMode = AppearanceEditMode.ELEMENT,
                activeElementTarget = target,
                fontsVisible = false,
                invalidRgbChannels = emptySet(),
                hexInputInvalid = false,
            ),
        )

    fun updateFont(fontFamily: AppearanceFontFamily): AppearanceController {
        val spec = AppearanceTargetCatalogue.textTargets.first { it.target == state.activeTextTarget }
        val resolved = AppearanceFontResolver(state.availableFontFamilies).resolve(fontFamily, spec.defaultFontFamily)
        return copy(state = state.withDraft(state.currentDraft.withTextFont(state.activeTextTarget, resolved)))
    }

    fun updateColor(color: AppearanceColor): AppearanceController =
        copy(state = state.withDraft(state.currentDraft.withActiveColor(state, color)).copy(hexInputInvalid = false))

    fun updateRgb(
        channel: AppearanceRgbChannel,
        value: Int,
    ): AppearanceController =
        updateRgb(channel, value.toString())

    fun updateRgb(
        channel: AppearanceRgbChannel,
        value: String,
    ): AppearanceController {
        val parsed = value.toIntOrNull()
        if (parsed == null || parsed !in RGB_RANGE) {
            return copy(state = state.copy(invalidRgbChannels = state.invalidRgbChannels + channel))
        }

        val color = state.activeColor.withChannel(channel, parsed)
        return copy(
            state = state
                .withDraft(state.currentDraft.withActiveColor(state, color))
                .copy(invalidRgbChannels = state.invalidRgbChannels - channel, hexInputInvalid = false),
        )
    }

    fun updateHex(value: String): AppearanceController =
        when (val parsed = AppearanceColor.from(value)) {
            is AppearanceColorParseResult.Valid -> copy(
                state = state
                    .withDraft(state.currentDraft.withActiveColor(state, parsed.color))
                    .copy(hexInputInvalid = false),
            )
            is AppearanceColorParseResult.Invalid -> copy(state = state.copy(hexInputInvalid = true))
        }

    fun resetCurrentMode(): AppearanceController =
        when (val reset = runtime.appearanceProfileService.resetMode(state.mode)) {
            is AppearanceProfileResetResult.Reset -> copy(
                state = stateFrom(
                    mode = state.mode,
                    themePreference = state.themePreference,
                    profileState = reset.state,
                    availableFontFamilies = state.availableFontFamilies,
                    errorKey = null,
                ).copy(
                    expandedPanel = state.expandedPanel,
                    activeEditMode = state.activeEditMode,
                    activeTextTarget = state.activeTextTarget,
                    activeElementTarget = state.activeElementTarget,
                    fontsVisible = state.fontsVisible,
                ),
            )
            is AppearanceProfileResetResult.StorageFailed -> copy(
                state = state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
            )
        }

    fun openSaveThemeDialog(): AppearanceController =
        copy(
            state = state.copy(
                saveThemeDialogOpen = true,
                saveThemeMode = state.mode,
                saveThemeName = "",
                saveThemeNameFocusRequested = true,
            ),
        )

    fun closeSaveThemeDialog(): AppearanceController =
        copy(
            state = state.copy(
                saveThemeDialogOpen = false,
                saveThemeNameFocusRequested = false,
            ),
        )

    fun updateSaveThemeName(name: String): AppearanceController =
        copy(state = state.copy(saveThemeName = name))

    fun updateSaveThemeMode(mode: AppearanceMode): AppearanceController =
        copy(state = state.copy(saveThemeMode = mode))

    fun saveTheme(
        name: String,
        mode: AppearanceMode,
    ): AppearanceController {
        if (name.isBlank()) {
            return copy(
                state = state.copy(
                    saveThemeDialogOpen = true,
                    saveThemeName = name,
                    saveThemeNameFocusRequested = true,
                ),
            )
        }

        return when (val saved = runtime.appearanceProfileService.saveProfile(
            name = AppearanceProfileName(name),
            mode = mode,
            draft = state.currentDraft.copy(mode = mode),
        )) {
            is AppearanceProfileSaveResult.Saved -> {
                val preference = mode.toPreference()
                val errorKey = when (runtime.themePreferenceService.savePreference(preference)) {
                    ThemePreferenceSaveResult.Saved -> null
                    is ThemePreferenceSaveResult.StorageFailed -> GemTextKey.ThemePreferenceSaveFailed
                }
                copy(
                    state = stateFrom(
                        mode = mode,
                        themePreference = preference,
                        profileState = saved.state,
                        availableFontFamilies = state.availableFontFamilies,
                        currentDraft = AppearanceDraft.fromProfile(saved.profile),
                        errorKey = errorKey,
                    ).copy(
                        saveThemeDialogOpen = false,
                        saveThemeName = "",
                        saveThemeNameFocusRequested = false,
                    ),
                )
            }
            is AppearanceProfileSaveResult.Rejected -> copy(
                state = state.copy(
                    saveThemeDialogOpen = true,
                    saveThemeName = name,
                    saveThemeNameFocusRequested = true,
                ),
            )
            is AppearanceProfileSaveResult.StorageFailed -> copy(
                state = state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
            )
        }
    }

    fun setExpandedPanel(panel: AppearanceExpandedPanel): AppearanceController =
        copy(state = state.copy(expandedPanel = panel))

    private fun stateFrom(
        mode: AppearanceMode,
        themePreference: ThemePreference,
        profileState: AppearanceProfileState,
        availableFontFamilies: List<AppearanceFontFamily>,
        currentDraft: AppearanceDraft = profileState.selectedDraftFor(mode) ?: systemDraft(mode, availableFontFamilies),
        errorKey: GemTextKey?,
    ): AppearanceUiState =
        state.copy(
            mode = mode,
            themePreference = themePreference,
            selectedProfileId = currentDraft.selectedProfileId,
            stockProfiles = profileState.stockProfiles,
            customProfiles = profileState.customProfiles,
            availableFontFamilies = availableFontFamilies,
            currentDraft = currentDraft,
            invalidRgbChannels = emptySet(),
            hexInputInvalid = false,
            saveThemeMode = mode,
            profileWarning = profileState.warning,
            errorKey = errorKey,
        )

    private fun systemDraft(
        mode: AppearanceMode,
        availableFontFamilies: List<AppearanceFontFamily>,
    ): AppearanceDraft =
        AppearanceDraft.fromProfile(
            GemSystemThemeProfileFactory.profile(
                mode = mode,
                availableFontFamilies = availableFontFamilies,
                platformSystemFontFamilyProvider = runtime.platformSystemFontFamilyProvider,
            ),
        ).copy(selectedProfileId = null)

    private fun copy(state: AppearanceUiState): AppearanceController =
        AppearanceController(runtime, state)

    companion object {
        fun initial(
            runtime: GemUiRuntime,
            osDark: Boolean,
        ): AppearanceController =
            AppearanceController(
                runtime = runtime,
                state = AppearanceUiState.loading(osDark),
            ).refresh(osDark)

        private val RGB_RANGE = 0..255

        private fun resolve(
            preference: ThemePreference,
            osDark: Boolean,
        ): AppearanceMode =
            when (preference) {
                ThemePreference.SYSTEM -> if (osDark) AppearanceMode.DARK else AppearanceMode.LIGHT
                ThemePreference.LIGHT -> AppearanceMode.LIGHT
                ThemePreference.DARK -> AppearanceMode.DARK
            }
    }
}

private fun AppearanceProfileLoadResult.state(): AppearanceProfileState =
    when (this) {
        is AppearanceProfileLoadResult.Loaded -> state
        is AppearanceProfileLoadResult.StorageFailed -> state
    }

private fun AppearanceMode.toPreference(): ThemePreference =
    when (this) {
        AppearanceMode.LIGHT -> ThemePreference.LIGHT
        AppearanceMode.DARK -> ThemePreference.DARK
    }

private fun AppearanceUiState.withDraft(draft: AppearanceDraft): AppearanceUiState =
    copy(
        currentDraft = draft.copy(selectedProfileId = null, dirty = true),
        selectedProfileId = null,
        errorKey = null,
    )

private fun AppearanceDraft.withTextFont(
    target: AppearanceTextTarget,
    family: AppearanceFontFamily,
): AppearanceDraft =
    copy(
        textFonts = textFonts + (target to family),
        selectedProfileId = null,
        dirty = true,
    )

private fun AppearanceDraft.withActiveColor(
    state: AppearanceUiState,
    color: AppearanceColor,
): AppearanceDraft =
    when (state.activeEditMode) {
        AppearanceEditMode.TEXT -> copy(textColors = textColors + (state.activeTextTarget to color))
        AppearanceEditMode.ELEMENT -> copy(elementColors = elementColors + (state.activeElementTarget to color))
    }

private fun AppearanceColor.withChannel(
    channel: AppearanceRgbChannel,
    value: Int,
): AppearanceColor {
    val rgb = this.value.removePrefix("#")
    val red = rgb.substring(0, 2).toInt(16)
    val green = rgb.substring(2, 4).toInt(16)
    val blue = rgb.substring(4, 6).toInt(16)
    val next = when (channel) {
        AppearanceRgbChannel.R -> Triple(value, green, blue)
        AppearanceRgbChannel.G -> Triple(red, value, blue)
        AppearanceRgbChannel.B -> Triple(red, green, value)
    }
    return AppearanceColor.require("#${next.first.hexByte()}${next.second.hexByte()}${next.third.hexByte()}")
}

private fun Int.hexByte(): String =
    toString(radix = 16).uppercase().padStart(length = 2, padChar = '0')
