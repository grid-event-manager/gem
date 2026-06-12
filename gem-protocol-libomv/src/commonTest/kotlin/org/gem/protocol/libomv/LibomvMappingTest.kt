package org.gem.protocol.libomv

import org.gem.core.domain.GroupSendState
import org.gem.protocol.libomv.mapping.LibomvLoginMapping
import org.gem.protocol.libomv.mapping.LibomvLoginMappingResult
import org.gem.protocol.libomv.mapping.LoginKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibomvMappingTest {
    @Test
    fun `maps login response simulator and circuit fields`() {
        val mapped = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(loginResponseBody()),
        ).value

        assertEquals("live-session", mapped.sessionId.value)
        assertEquals("agent-id", mapped.agentId)
        assertEquals(secureUrl("caps.example", "/seed"), mapped.seedCapability)
        assertEquals("203.0.113.8", mapped.simulatorIp)
        assertEquals(13000, mapped.simulatorPort)
        assertEquals((1024L shl 32) or 2048L, mapped.regionHandle)
        assertEquals(123456789L, mapped.circuitCode)
    }

    @Test
    fun `login response can succeed without current-groups identity fields`() {
        val mapped = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(loginResponseBody(includeSimulatorFields = false)),
        ).value

        assertEquals("live-session", mapped.sessionId.value)
        assertEquals("agent-id", mapped.agentId)
        assertEquals(secureUrl("caps.example", "/seed"), mapped.seedCapability)
        assertNull(mapped.simulatorIp)
        assertNull(mapped.simulatorPort)
        assertNull(mapped.regionHandle)
        assertNull(mapped.circuitCode)
    }

    @Test
    fun `malformed login simulator fields do not become current-groups identity`() {
        val mapped = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                loginResponseBody(
                    simulatorPort = "not-a-port",
                    regionX = "not-a-region",
                    circuitCode = "-1",
                ),
            ),
        ).value

        assertEquals("203.0.113.8", mapped.simulatorIp)
        assertNull(mapped.simulatorPort)
        assertNull(mapped.regionHandle)
        assertNull(mapped.circuitCode)
    }

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

    private fun loginResponseBody(
        includeSimulatorFields: Boolean = true,
        simulatorPort: String = "13000",
        regionX: String = "1024",
        regionY: String = "2048",
        circuitCode: String = "123456789",
    ): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "true")
        field(LoginKeys.AGENT_ID, "agent-id")
        field(LoginKeys.SESSION_ID, "live-session")
        field(LoginKeys.SEED_CAPABILITY, secureUrl("caps.example", "/seed"))
        if (includeSimulatorFields) {
            field(LoginKeys.SIM_IP, "203.0.113.8")
            integer(LoginKeys.SIM_PORT, simulatorPort)
            integer(LoginKeys.REGION_X, regionX)
            integer(LoginKeys.REGION_Y, regionY)
            integer(LoginKeys.CIRCUIT_CODE, circuitCode)
        }
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(key).append("</key><string>").append(value).append("</string>")
    }

    private fun StringBuilder.integer(key: String, value: String) {
        append("<key>").append(key).append("</key><integer>").append(value).append("</integer>")
    }

    private fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
}
