package org.hostess.preferences

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidHostessPreferencePathsTest {
    @Test
    fun `places preference file under android app files dir`() {
        val path = AndroidHostessPreferencePaths.defaultPreferenceFile("/data/user/0/org.hostess/files")

        assertEquals("/data/user/0/org.hostess/files/Hostess/preferences/ui.properties", path)
    }
}
