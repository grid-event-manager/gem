package org.hostess.protocol.libomv

import libomv.packets.GroupNoticeRequestPacket
import libomv.packets.PacketCatalog
import libomv.packets.PacketType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LibomvBootstrapTest {
    @Test
    fun `generated packet catalog compiles from promoted message template`() {
        assertEquals(476, PacketCatalog.PACKET_DEFINITION_COUNT)
        assertEquals(479, PacketCatalog.GENERATED_JAVA_FILE_COUNT)
        assertEquals(
            "2a351a754a379765bac2cebf5284692df3f869ce662756ab29733b6330cc668d",
            PacketCatalog.MESSAGE_TEMPLATE_SHA256,
        )
        assertEquals(PacketType.GroupNoticeRequest, GroupNoticeRequestPacket().type)
    }

    @Test
    fun `adapter session is creatable but protocol remains unavailable`() {
        val session = LibomvClientSession.unavailable()

        assertFalse(session.isProtocolAvailable())
    }
}
