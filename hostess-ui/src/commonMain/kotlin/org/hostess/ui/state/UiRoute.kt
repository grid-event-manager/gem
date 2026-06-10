package org.hostess.ui.state

sealed interface UiRoute {
    data object Login : UiRoute
    data object Compose : UiRoute
    data object Settings : UiRoute
}
