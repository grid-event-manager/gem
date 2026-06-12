package org.gem.protocol.libomv.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProtocolXmlTreeParserTest {
    @Test
    fun `parses declaration elements attributes entities and self closing tags`() {
        val root = parse(
            """
            <?xml version="1.0"?>
            <root ignored="true">
              <child attr="value > ignored">one &amp; &lt; &quot; &apos; &gt;</child>
              <empty />
            </root>
            """.trimIndent(),
        )

        assertEquals("root", root.name)
        assertEquals("child", root.children[0].name)
        assertEquals("one & < \" ' >", root.children[0].text.trim())
        assertEquals("empty", root.children[1].name)
        assertEquals(emptyList(), root.children[1].children)
    }

    @Test
    fun `rejects document type and comments`() {
        assertNull(ProtocolXmlTreeParser.parse("<!DOCTYPE root><root />".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<root><!-- comment --></root>".encodeToByteArray()))
    }

    @Test
    fun `rejects processing instructions after declaration position`() {
        assertNull(ProtocolXmlTreeParser.parse("<?xml-stylesheet href=\"x\"?><root />".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<root><?target value?></root>".encodeToByteArray()))
    }

    @Test
    fun `rejects malformed entity references`() {
        assertNull(ProtocolXmlTreeParser.parse("<root>&unsafe;</root>".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<root>&amp</root>".encodeToByteArray()))
    }

    @Test
    fun `rejects unclosed mismatched and duplicate roots`() {
        assertNull(ProtocolXmlTreeParser.parse("<root><child></root>".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<root><child /></other>".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<one /><two />".encodeToByteArray()))
    }

    @Test
    fun `rejects invalid element names`() {
        assertNull(ProtocolXmlTreeParser.parse("<1root />".encodeToByteArray()))
        assertNull(ProtocolXmlTreeParser.parse("<-root />".encodeToByteArray()))
    }

    @Test
    fun `rejects malformed self closing tag spacing`() {
        assertNull(ProtocolXmlTreeParser.parse("<root/ >".encodeToByteArray()))
    }

    @Test
    fun `rejects mixed content around child elements`() {
        assertNull(ProtocolXmlTreeParser.parse("<root>value<child /></root>".encodeToByteArray()))
    }

    private fun parse(text: String): ProtocolXmlElement =
        assertNotNull(ProtocolXmlTreeParser.parse(text.encodeToByteArray()))
}
