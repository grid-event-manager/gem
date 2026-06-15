package org.gem.ui.state

sealed interface UiRoute {
    data object Login : UiRoute
    data object Compose : UiRoute
    data object Accounts : UiRoute
    data object Settings : UiRoute
}
