package org.gem.ui.text

fun interface PlatformLocaleProvider {
    fun currentLocaleTag(): String
}
