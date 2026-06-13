package org.gem.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemDesktopRenderPolicyTest {
    @Test
    fun `windows defaults to software renderer when no override exists`() {
        val writes = mutableMapOf<String, String>()

        GemDesktopRenderPolicy.install(
            osName = "Windows 11",
            readProperty = { null },
            writeProperty = { key, value ->
                writes[key] = value
                null
            },
        )

        assertEquals("SOFTWARE", writes["skiko.renderApi"])
    }

    @Test
    fun `windows preserves explicit renderer override`() {
        val writes = mutableMapOf<String, String>()

        GemDesktopRenderPolicy.install(
            osName = "Windows 11",
            readProperty = { "OPENGL" },
            writeProperty = { key, value ->
                writes[key] = value
                null
            },
        )

        assertTrue(writes.isEmpty())
    }

    @Test
    fun `non windows platforms do not receive renderer override`() {
        val writes = mutableMapOf<String, String>()

        GemDesktopRenderPolicy.install(
            osName = "Mac OS X",
            readProperty = { null },
            writeProperty = { key, value ->
                writes[key] = value
                null
            },
        )

        assertTrue(writes.isEmpty())
    }
}
