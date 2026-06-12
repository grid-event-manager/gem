package org.gem.preferences

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidGemPreferencePathsTest {
    @Test
    fun `places preference file under android app files dir`() {
        val path = AndroidGemPreferencePaths.defaultPreferenceFile("/data/user/0/org.gem/files")

        assertEquals("/data/user/0/org.gem/files/gem/preferences/ui.properties", path)
    }
}
