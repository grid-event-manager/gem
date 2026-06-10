package org.hostess.ui.controllers

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.ui.design.ResolvedThemeMode
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeThemePreferenceStore
import org.hostess.ui.text.HostessTextKey
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
        assertEquals(HostessTextKey.ThemePreferenceUnavailable, invalid.state.errorKey)
        assertEquals(ThemePreference.SYSTEM, storage.state.preference)
        assertEquals(ResolvedThemeMode.LIGHT, storage.state.resolvedMode)
        assertEquals(HostessTextKey.ThemePreferenceUnavailable, storage.state.errorKey)
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
        assertEquals(HostessTextKey.ThemePreferenceSaveFailed, failed.state.errorKey)
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
        ThemeController.initial(FakeHostessUiRuntime.ready(themePreferenceStore = store), osDark)
}
