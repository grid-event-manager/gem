package org.gem.ui.controllers

import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.ui.design.ResolvedThemeMode
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeThemePreferenceStore
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeControllerTest {
    @Test
    fun systemPreferenceFollowsOsTheme() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)

        assertEquals(ResolvedThemeMode.LIGHT, controller(store, osDark = false).state.resolvedMode)
        assertEquals(ResolvedThemeMode.DARK, controller(store, osDark = true).state.resolvedMode)
    }

    @Test
    fun savedLightAndDarkOverrideOsTheme() {
        val lightStore = FakeThemePreferenceStore(ThemePreferenceLoadResult.Loaded(ThemePreference.LIGHT))
        val darkStore = FakeThemePreferenceStore(ThemePreferenceLoadResult.Loaded(ThemePreference.DARK))

        assertEquals(ResolvedThemeMode.LIGHT, controller(lightStore, osDark = true).state.resolvedMode)
        assertEquals(ResolvedThemeMode.DARK, controller(darkStore, osDark = false).state.resolvedMode)
    }

    @Test
    fun invalidAndStorageLoadWarningsResolveSystemWithUnavailableError() {
        val invalid = controller(
            FakeThemePreferenceStore(ThemePreferenceLoadResult.InvalidValue("BLUE")),
            osDark = true,
        )
        val storage = controller(
            FakeThemePreferenceStore(ThemePreferenceLoadResult.StorageFailed("storage_failed")),
            osDark = false,
        )

        assertEquals(ThemePreference.SYSTEM, invalid.state.preference)
        assertEquals(ResolvedThemeMode.DARK, invalid.state.resolvedMode)
        assertEquals(GemTextKey.ThemePreferenceUnavailable, invalid.state.errorKey)
        assertEquals(ThemePreference.SYSTEM, storage.state.preference)
        assertEquals(ResolvedThemeMode.LIGHT, storage.state.resolvedMode)
        assertEquals(GemTextKey.ThemePreferenceUnavailable, storage.state.errorKey)
    }

    @Test
    fun manualThemeSaveUpdatesPreferenceAndClearsErrors() {
        val store = FakeThemePreferenceStore(ThemePreferenceLoadResult.Missing)
        val updated = controller(store, osDark = false).setManualTheme(ResolvedThemeMode.DARK, osDark = false)

        assertEquals(ThemePreference.DARK, store.lastSavedPreference)
        assertEquals(ThemePreference.DARK, updated.state.preference)
        assertEquals(ResolvedThemeMode.DARK, updated.state.resolvedMode)
        assertNull(updated.state.errorKey)
    }

    @Test
    fun manualThemeSaveFailureKeepsPreviousModeAndReportsError() {
        val store = FakeThemePreferenceStore(
            loadResult = ThemePreferenceLoadResult.Loaded(ThemePreference.LIGHT),
            saveResult = ThemePreferenceSaveResult.StorageFailed("storage_failed"),
        )
        val original = controller(store, osDark = true)

        val failed = original.setManualTheme(ResolvedThemeMode.DARK, osDark = true)

        assertEquals(ThemePreference.DARK, store.lastSavedPreference)
        assertEquals(ThemePreference.LIGHT, failed.state.preference)
        assertEquals(ResolvedThemeMode.LIGHT, failed.state.resolvedMode)
        assertEquals(GemTextKey.ThemePreferenceSaveFailed, failed.state.errorKey)
    }

    @Test
    fun toggleCheckedReflectsResolvedDarkMode() {
        assertEquals(false, controller(FakeThemePreferenceStore(), osDark = false).state.toggleChecked)
        assertEquals(true, controller(FakeThemePreferenceStore(), osDark = true).state.toggleChecked)
    }

    private fun controller(
        store: FakeThemePreferenceStore,
        osDark: Boolean,
    ): ThemeController =
        ThemeController.initial(FakeGemUiRuntime.ready(themePreferenceStore = store), osDark)
}
