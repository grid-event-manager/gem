package org.hostess.protocol.libomv

import org.hostess.core.domain.GroupSendState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibomvMappingTest {
    @Test
    fun `maps group ID name powers and notice preference into Hostess membership`() {
        val membership = LibomvMapping.groupMembership(
            LibomvGroupSnapshot(
                groupId = "group-id",
                displayName = "Music Room",
                powers = LibomvMapping.SEND_NOTICES_POWER,
                acceptsNotices = true,
            ),
        )

        assertEquals("group-id", membership.groupId.value)
        assertEquals("Music Room", membership.displayName.value)
        assertTrue(membership.canSendNotices)
        assertEquals(true, membership.acceptsNotices)
    }

    @Test
    fun `maps absent powers and missing notice preference without guessing`() {
        val membership = LibomvMapping.groupMembership(
            LibomvGroupSnapshot(
                groupId = "group-id",
                displayName = "Silent Group",
                powers = 0L,
                acceptsNotices = null,
            ),
        )

        assertFalse(membership.canSendNotices)
        assertNull(membership.acceptsNotices)
    }

    @Test
    fun `maps group notice status without raw protocol leakage`() {
        val sent = LibomvMapping.groupNoticeStatus(
            LibomvNoticeStatusSnapshot(
                group = LibomvGroupSnapshot("group-id", "Music Room", LibomvMapping.SEND_NOTICES_POWER, true),
                delivered = true,
                detail = null,
            ),
        )
        val failed = LibomvMapping.groupNoticeStatus(
            LibomvNoticeStatusSnapshot(
                group = LibomvGroupSnapshot("group-id", "Music Room", LibomvMapping.SEND_NOTICES_POWER, true),
                delivered = false,
                detail = "protocol unavailable",
            ),
        )

        assertEquals(GroupSendState.SENT, sent.state)
        assertEquals(GroupSendState.FAILED, failed.state)
        assertEquals("protocol unavailable", failed.detail)
    }
}
