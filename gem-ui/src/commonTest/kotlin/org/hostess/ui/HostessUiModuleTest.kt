package org.hostess.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class HostessUiModuleTest {
    @Test
    fun moduleNameMatchesGradleModule() {
        assertEquals("hostess-ui", HostessUiModule.MODULE_NAME)
    }
}
