package org.gem.ui.controllers

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileStore
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSaveResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.ui.state.AppearanceEditMode
import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.state.AppearanceRgbChannel
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeThemePreferenceStore
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppearanceControllerTest {
    @Test
    fun initialLoadsThemeProfilesAndPlatformFonts() {
        val runtime = FakeGemUiRuntime.ready(
            themePreferenceStore = FakeThemePreferenceStore(ThemePreferenceLoadResult.Loaded(ThemePreference.DARK)),
            availableFontFamilies = listOf(AppearanceFontFamily("Noto Sans"), AppearanceFontFamily("sans-serif")),
            platformSystemFontFamilyProvider = { families ->
                families.first { it.value == "Noto Sans" }
            },
        )

        val controller = AppearanceController.initial(runtime, osDark = false)

        assertEquals(AppearanceMode.DARK, controller.state.mode)
        assertEquals(ThemePreference.DARK, controller.state.themePreference)
        assertEquals(6, controller.state.stockProfiles.size)
        assertEquals(listOf("Noto Sans", "sans-serif"), controller.state.availableFontFamilies.map { it.value })
        assertNull(controller.state.selectedProfileId)
        assertTrue(controller.state.currentDraft.textFonts.values.all { it == AppearanceFontFamily("Noto Sans") })
        assertNull(controller.state.errorKey)
    }

    @Test
    fun setManualThemeSavesPreferenceAndPreservesStockProfileFamily() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)
        val runtime = FakeGemUiRuntime.ready(themePreferenceStore = store)
        val selected = AppearanceController.initial(runtime, osDark = false)
            .selectProfile(AppearanceProfileId("stock-goth-dark"))

        val controller = selected.setManualTheme(AppearanceMode.LIGHT, osDark = true)

        assertEquals(ThemePreference.LIGHT, store.lastSavedPreference)
        assertEquals(AppearanceMode.LIGHT, controller.state.mode)
        assertEquals(AppearanceProfileId("stock-goth-light"), controller.state.selectedProfileId)
        assertEquals("Georgia", controller.state.currentDraft.textFonts.getValue(AppearanceTextTarget.TITLE_BAR).value)
        assertFalse(controller.state.currentDraft.dirty)
    }

    @Test
    fun setManualThemeFallsBackToSystemOnlyWhenCustomCounterpartIsAbsentAndRestoresFamilyWhenReturning() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)
        val runtime = FakeGemUiRuntime.ready(themePreferenceStore = store)
        val darkCustom = AppearanceController.initial(runtime, osDark = false)
            .selectProfile(AppearanceProfileId("stock-goth-dark"))
            .saveTheme("Venue", AppearanceMode.DARK)

        val light = darkCustom.setManualTheme(AppearanceMode.LIGHT, osDark = true)
        val dark = light.setManualTheme(AppearanceMode.DARK, osDark = false)

        assertEquals(AppearanceMode.LIGHT, light.state.mode)
        assertNull(light.state.selectedProfileId)
        assertTrue(light.state.currentDraft.textFonts.values.all { it == AppearanceFontFamily("sans-serif") })
        assertEquals(AppearanceMode.DARK, dark.state.mode)
        assertEquals(AppearanceProfileId("custom:dark:venue"), dark.state.selectedProfileId)
    }

    @Test
    fun selectProfileChangesModeAndSavesMatchingManualTheme() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)
        val runtime = FakeGemUiRuntime.ready(themePreferenceStore = store)

        val controller = AppearanceController.initial(runtime, osDark = false)
            .openSaveThemeDialog()
            .selectProfile(AppearanceProfileId("stock-goth-dark"))

        assertEquals(AppearanceMode.DARK, controller.state.mode)
        assertEquals(ThemePreference.DARK, controller.state.themePreference)
        assertEquals(ThemePreference.DARK, store.lastSavedPreference)
        assertEquals(AppearanceProfileId("stock-goth-dark"), controller.state.selectedProfileId)
        assertEquals("Georgia", controller.state.currentDraft.textFonts.getValue(AppearanceTextTarget.TITLE_BAR).value)
        assertFalse(controller.state.saveThemeDialogOpen)
    }

    @Test
    fun manualFontAndColourEditsUpdateDraftAndValidationFlags() {
        val runtime = FakeGemUiRuntime.ready(
            availableFontFamilies = listOf(AppearanceFontFamily("Display")),
        )
        val initial = AppearanceController.initial(runtime, osDark = false)
            .updateTextTarget(AppearanceTextTarget.FIELD_TEXT)
            .updateColor(AppearanceColor.require("#444444"))
            .updateFont(AppearanceFontFamily("display"))

        assertEquals(AppearanceEditMode.TEXT, initial.state.activeEditMode)
        assertTrue(initial.state.fontsVisible)
        assertEquals(AppearanceFontFamily("Display"), initial.state.currentDraft.textFonts[AppearanceTextTarget.FIELD_TEXT])
        assertTrue(initial.state.currentDraft.dirty)

        val invalid = initial.updateRgb(AppearanceRgbChannel.R, "300")
        assertEquals(setOf(AppearanceRgbChannel.R), invalid.state.invalidRgbChannels)
        assertEquals(initial.state.currentDraft.textColors, invalid.state.currentDraft.textColors)

        val valid = invalid.updateRgb(AppearanceRgbChannel.R, "17")
        assertTrue(AppearanceRgbChannel.R !in valid.state.invalidRgbChannels)
        assertEquals("#114444", valid.state.currentDraft.textColors.getValue(AppearanceTextTarget.FIELD_TEXT).value)

        val element = valid.updateElementTarget(AppearanceElementTarget.PAGE_BACKGROUND)
            .updateHex("#123456")
        assertEquals(AppearanceEditMode.ELEMENT, element.state.activeEditMode)
        assertFalse(element.state.fontsVisible)
        assertEquals("#123456", element.state.currentDraft.elementColors.getValue(AppearanceElementTarget.PAGE_BACKGROUND).value)

        val badHex = element.updateHex("not-a-colour")
        assertTrue(badHex.state.hexInputInvalid)
        assertEquals(element.state.currentDraft.elementColors, badHex.state.currentDraft.elementColors)
    }

    @Test
    fun targetOpenPreservesVisiblePickerColourAndTogglesFonts() {
        val textColour = AppearanceColor.require("#224466")
        val elementActive = AppearanceController.initial(FakeGemUiRuntime.ready(), osDark = false)
            .updateColor(textColour)
            .updateElementTarget(AppearanceElementTarget.PAGE_BACKGROUND)

        assertEquals(AppearanceEditMode.ELEMENT, elementActive.state.activeEditMode)
        assertFalse(elementActive.state.fontsVisible)
        assertEquals(textColour, elementActive.state.activeColor)

        val elementColour = AppearanceColor.require("#AA5500")
        val textOpened = elementActive
            .updateColor(elementColour)
            .updateTextTarget(elementActive.state.activeTextTarget)

        assertEquals(AppearanceEditMode.TEXT, textOpened.state.activeEditMode)
        assertTrue(textOpened.state.fontsVisible)
        assertEquals(elementColour, textOpened.state.activeColor)
        assertEquals(elementColour, textOpened.state.currentDraft.textColors.getValue(AppearanceTextTarget.TITLE_BAR))

        val elementOpened = textOpened.updateElementTarget(textOpened.state.activeElementTarget)

        assertEquals(AppearanceEditMode.ELEMENT, elementOpened.state.activeEditMode)
        assertFalse(elementOpened.state.fontsVisible)
        assertEquals(elementColour, elementOpened.state.activeColor)
        assertEquals(elementColour, elementOpened.state.currentDraft.elementColors.getValue(AppearanceElementTarget.PAGE_BACKGROUND))
    }

    @Test
    fun targetChangesCarryCurrentPickerColourIntoSelectedTarget() {
        val carriedTextColour = AppearanceColor.require("#123456")
        val textChanged = AppearanceController.initial(FakeGemUiRuntime.ready(), osDark = false)
            .updateColor(carriedTextColour)
            .updateTextTarget(AppearanceTextTarget.BACK_BUTTON)

        assertEquals(AppearanceEditMode.TEXT, textChanged.state.activeEditMode)
        assertTrue(textChanged.state.fontsVisible)
        assertEquals(carriedTextColour, textChanged.state.activeColor)
        assertEquals(carriedTextColour, textChanged.state.currentDraft.textColors.getValue(AppearanceTextTarget.BACK_BUTTON))

        val carriedElementColour = AppearanceColor.require("#654321")
        val elementChanged = textChanged
            .updateColor(carriedElementColour)
            .updateElementTarget(AppearanceElementTarget.FIELD_BACKGROUND)

        assertEquals(AppearanceEditMode.ELEMENT, elementChanged.state.activeEditMode)
        assertFalse(elementChanged.state.fontsVisible)
        assertEquals(carriedElementColour, elementChanged.state.activeColor)
        assertEquals(
            carriedElementColour,
            elementChanged.state.currentDraft.elementColors.getValue(AppearanceElementTarget.FIELD_BACKGROUND),
        )
    }

    @Test
    fun fontUpdatesApplyImmediatelyToBackAndThemeToggleTargets() {
        val runtime = FakeGemUiRuntime.ready(
            availableFontFamilies = listOf(AppearanceFontFamily("Display"), AppearanceFontFamily("sans-serif")),
        )
        val back = AppearanceController.initial(runtime, osDark = false)
            .updateTextTarget(AppearanceTextTarget.BACK_BUTTON)
            .updateFont(AppearanceFontFamily("display"))

        assertEquals(
            AppearanceFontFamily("Display"),
            back.state.currentDraft.textFonts.getValue(AppearanceTextTarget.BACK_BUTTON),
        )

        val themeToggle = back
            .updateTextTarget(AppearanceTextTarget.THEME_TOGGLE_LABELS)
            .updateFont(AppearanceFontFamily("display"))

        assertEquals(
            AppearanceFontFamily("Display"),
            themeToggle.state.currentDraft.textFonts.getValue(AppearanceTextTarget.THEME_TOGGLE_LABELS),
        )
    }

    @Test
    fun saveThemeModalKeepsBlankNamesFocusedAndSelectsSavedProfileOnSuccess() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)
        val runtime = FakeGemUiRuntime.ready(themePreferenceStore = store)
        val opened = AppearanceController.initial(runtime, osDark = false).openSaveThemeDialog()
        val darkSaveMode = opened.updateSaveThemeMode(AppearanceMode.DARK)

        assertEquals(AppearanceMode.DARK, darkSaveMode.state.saveThemeMode)

        val rejected = darkSaveMode.saveTheme("   ", AppearanceMode.LIGHT)

        assertTrue(rejected.state.saveThemeDialogOpen)
        assertTrue(rejected.state.saveThemeNameFocusRequested)

        val saved = rejected.saveTheme("My Theme", AppearanceMode.LIGHT)

        assertFalse(saved.state.saveThemeDialogOpen)
        assertEquals("", saved.state.saveThemeName)
        assertFalse(saved.state.saveThemeNameFocusRequested)
        assertNotNull(saved.state.selectedProfileId)
        assertEquals("custom:light:my-theme", saved.state.selectedProfileId.value)
        assertEquals(ThemePreference.LIGHT, store.lastSavedPreference)
    }

    @Test
    fun redPathsReportTypedStateErrors() {
        val saveFailingThemeStore = FakeThemePreferenceStore(
            loadResult = ThemePreferenceLoadResult.Missing,
            saveResult = ThemePreferenceSaveResult.StorageFailed("no-write"),
        )
        val themeFailure = AppearanceController.initial(
            FakeGemUiRuntime.ready(themePreferenceStore = saveFailingThemeStore),
            osDark = false,
        ).setManualTheme(AppearanceMode.DARK, osDark = false)

        assertEquals(GemTextKey.ThemePreferenceSaveFailed, themeFailure.state.errorKey)

        val profileFailure = AppearanceController.initial(
            FakeGemUiRuntime.ready(appearanceProfileStore = StorageFailingAppearanceProfileStore()),
            osDark = false,
        )

        assertEquals(GemTextKey.ThemePreferenceUnavailable, profileFailure.state.errorKey)
    }

    @Test
    fun resetCurrentModeClearsActiveProfileAndUsesHiddenSystemDraft() {
        val controller = AppearanceController.initial(FakeGemUiRuntime.ready(), osDark = false)
            .selectProfile(AppearanceProfileId("stock-princess-light"))
            .setExpandedPanel(AppearanceExpandedPanel.CUSTOMISE)
            .resetCurrentMode()

        assertNull(controller.state.selectedProfileId)
        assertEquals(AppearanceMode.LIGHT, controller.state.mode)
        assertTrue(controller.state.currentDraft.textFonts.values.all { it == AppearanceFontFamily("sans-serif") })
        assertEquals(AppearanceExpandedPanel.CUSTOMISE, controller.state.expandedPanel)
    }

    @Test
    fun manualThemeResetStorageFailureShowsRequestedSystemDraftAndError() {
        val controller = AppearanceController.initial(
            FakeGemUiRuntime.ready(
                appearanceProfileStore = ResetStorageFailingAppearanceProfileStore(),
            ),
            osDark = false,
        ).setManualTheme(AppearanceMode.DARK, osDark = false)

        assertEquals(AppearanceMode.DARK, controller.state.mode)
        assertEquals(ThemePreference.DARK, controller.state.themePreference)
        assertNull(controller.state.selectedProfileId)
        assertTrue(controller.state.currentDraft.textFonts.values.all { it == AppearanceFontFamily("sans-serif") })
        assertEquals(GemTextKey.ThemePreferenceSaveFailed, controller.state.errorKey)
    }

    @Test
    fun invalidActiveProfileIdFallsBackToHiddenSystemDraft() {
        val controller = AppearanceController.initial(
            FakeGemUiRuntime.ready(
                appearanceProfileStore = SnapshotAppearanceProfileStore(
                    AppearanceProfileStoreSnapshot(
                        customProfiles = emptyList(),
                        activeLightProfileId = AppearanceProfileId("missing-profile"),
                        activeDarkProfileId = null,
                    ),
                ),
            ),
            osDark = false,
        )

        assertNull(controller.state.selectedProfileId)
        assertTrue(controller.state.currentDraft.textFonts.values.all { it == AppearanceFontFamily("sans-serif") })
    }

    @Test
    fun expandedPanelIsStateOnly() {
        val controller = AppearanceController.initial(FakeGemUiRuntime.ready(), osDark = false)
            .setExpandedPanel(AppearanceExpandedPanel.CUSTOMISE)

        assertEquals(AppearanceExpandedPanel.CUSTOMISE, controller.state.expandedPanel)
    }
}

private class StorageFailingAppearanceProfileStore : AppearanceProfileStore {
    override fun load(): AppearanceProfileStoreLoadResult =
        AppearanceProfileStoreLoadResult.StorageFailed("no-read")

    override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult =
        AppearanceProfileStoreSaveResult.StorageFailed("no-write")
}

private class ResetStorageFailingAppearanceProfileStore : AppearanceProfileStore {
    override fun load(): AppearanceProfileStoreLoadResult =
        AppearanceProfileStoreLoadResult.Loaded(
            AppearanceProfileStoreSnapshot(
                customProfiles = emptyList(),
                activeLightProfileId = AppearanceProfileId("stock-princess-light"),
                activeDarkProfileId = null,
            ),
        )

    override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult =
        AppearanceProfileStoreSaveResult.StorageFailed("no-write")
}

private class SnapshotAppearanceProfileStore(
    private val snapshot: AppearanceProfileStoreSnapshot,
) : AppearanceProfileStore {
    override fun load(): AppearanceProfileStoreLoadResult =
        AppearanceProfileStoreLoadResult.Loaded(snapshot)

    override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult =
        AppearanceProfileStoreSaveResult.Saved
}
