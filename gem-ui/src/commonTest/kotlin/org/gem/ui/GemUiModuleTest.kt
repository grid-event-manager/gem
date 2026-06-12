package org.gem.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class GemUiModuleTest {
    @Test
    fun moduleNameMatchesGradleModule() {
        assertEquals("gem-ui", GemUiModule.MODULE_NAME)
    }
}
