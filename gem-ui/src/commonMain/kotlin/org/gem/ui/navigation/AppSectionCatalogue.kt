package org.gem.ui.navigation

import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextKey

object AppSectionCatalogue {
    val login = AppSection(
        sectionId = "login",
        route = UiRoute.Login,
        labelKey = GemTextKey.Login,
        backPolicy = SectionBackPolicy.Root,
        sessionStripPolicy = SectionSessionStripPolicy.UseAppState,
    )

    val compose = AppSection(
        sectionId = "compose",
        route = UiRoute.Compose,
        labelKey = GemTextKey.AppName,
        backPolicy = SectionBackPolicy.Root,
        sessionStripPolicy = SectionSessionStripPolicy.UseAppState,
    )

    val accounts = AppSection(
        sectionId = "accounts",
        route = UiRoute.Accounts,
        labelKey = GemTextKey.Accounts,
        backPolicy = SectionBackPolicy.ReturnToSessionOrLogin,
        sessionStripPolicy = SectionSessionStripPolicy.Hidden,
    )

    val settings = AppSection(
        sectionId = "settings",
        route = UiRoute.Settings,
        labelKey = GemTextKey.Settings,
        backPolicy = SectionBackPolicy.ReturnToSessionOrLogin,
        sessionStripPolicy = SectionSessionStripPolicy.Hidden,
    )

    val sections: List<AppSection> = listOf(login, compose, accounts, settings)

    fun sectionFor(route: UiRoute): AppSection =
        when (route) {
            UiRoute.Login -> login
            UiRoute.Compose -> compose
            UiRoute.Accounts -> accounts
            UiRoute.Settings -> settings
        }
}
