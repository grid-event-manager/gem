package org.gem.apps.android

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidPlatformLocaleProviderTest {
    @Test
    fun usesFirstAndroidLocaleTag() {
        assertEquals(
            "fr-FR",
            AndroidPlatformLocaleProvider.currentLocaleTag(
                orderedLocaleTags = listOf("fr-FR", "en-GB"),
                javaDefaultLocaleTag = { "en-GB" },
            ),
        )
    }

    @Test
    fun fallsBackToJavaDefaultOnlyWhenAndroidLocaleListIsEmpty() {
        assertEquals(
            "en-US",
            AndroidPlatformLocaleProvider.currentLocaleTag(
                orderedLocaleTags = emptyList(),
                javaDefaultLocaleTag = { "en-US" },
            ),
        )
    }
}
