package org.gem.ui.testing

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.ui.controllers.AppearanceController
import org.gem.ui.state.AppearanceUiState

fun controllerBackedAppearanceState(
    osDark: Boolean = false,
    availableFontFamilies: List<AppearanceFontFamily> = listOf(AppearanceFontFamily("sans-serif")),
): AppearanceUiState =
    AppearanceController.initial(
        runtime = FakeGemUiRuntime.ready(availableFontFamilies = availableFontFamilies),
        osDark = osDark,
    ).state
