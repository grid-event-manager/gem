package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.LibomvMapping
import org.gem.protocol.libomv.llsd.LlsdValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibomvOsdLongParserTest {
    @Test
    fun `integer string returns value`() {
        assertEquals(42L, LibomvOsdLongParser.parse(LlsdValue.ScalarValue("42")))
    }

    @Test
    fun `real string floors to long`() {
        assertEquals(42L, LibomvOsdLongParser.parse(LlsdValue.ScalarValue("42.9")))
    }

    @Test
    fun `trimmed decimal string floors to long`() {
        assertEquals(7L, LibomvOsdLongParser.parse(LlsdValue.ScalarValue(" 7.8 ")))
    }

    @Test
    fun `base64 binary group powers return bit field`() {
        val powers = LibomvOsdLongParser.parse(LlsdValue.ScalarValue("AAA///////8="))

        assertEquals(70368744177663L, powers)
        assertTrue(powers and LibomvMapping.SEND_NOTICES_POWER != 0L)
    }

    @Test
    fun `invalid unsupported and missing values return zero`() {
        val values = listOf(
            null,
            LlsdValue.ScalarValue(""),
            LlsdValue.ScalarValue("not-a-number"),
            LlsdValue.ScalarValue("NaN"),
            LlsdValue.ScalarValue("Infinity"),
            LlsdValue.ScalarValue("-Infinity"),
            LlsdValue.BooleanValue(true),
            LlsdValue.ArrayValue(emptyList()),
            LlsdValue.MapValue(emptyMap()),
            LlsdValue.Undefined,
        )

        values.forEach { value ->
            assertEquals(0L, LibomvOsdLongParser.parse(value))
        }
    }
}
