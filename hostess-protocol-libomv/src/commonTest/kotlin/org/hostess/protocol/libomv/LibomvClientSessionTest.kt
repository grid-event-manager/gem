package org.hostess.protocol.libomv

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.protocol.libomv.mapping.LoginAppearanceState
import org.hostess.protocol.libomv.mapping.LoginAppearanceStateResult
import org.hostess.protocol.libomv.mapping.LoginInventoryFolder
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.mapping.LoginInventoryRootsResult
import org.hostess.protocol.libomv.transport.CapabilityCache
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class LibomvClientSessionTest {
    @Test
    fun `matching active Hostess session passes binding check`() {
        val session = hostessSession("live-session")
        val clientSession = activeSession(session)

        assertNull(clientSession.requireSession(session))
        val identity = assertIs<LibomvSessionIdentityResult.Success>(clientSession.requireIdentity(session)).identity
        assertEquals("agent-id", identity.agentId)
        assertEquals("live-session", identity.sessionId)
        assertEquals("seed-capability", identity.seedCapability)
        assertEquals("203.0.113.8", identity.simulatorIp)
        assertEquals(13000, identity.simulatorPort)
        assertEquals(123456789L, identity.regionHandle)
        assertEquals(987654321L, identity.circuitCode)
        assertEquals("Venue Host", identity.agentName)
        assertEquals(
            CapabilityUrl("https://caps.example/event"),
            clientSession.capabilityCache(identity)?.urlFor(CapabilityName.EVENT_QUEUE_GET),
        )
        val roots = assertIs<LoginInventoryRootsResult.Success>(clientSession.inventoryRoots(session)).roots
        val appearanceState = assertIs<LoginAppearanceStateResult.Success>(
            clientSession.appearanceState(session),
        ).appearanceState
        assertEquals(LoginAppearanceState(agentAppearanceService = true, cofVersion = 51), appearanceState)
        assertEquals("inventory-root-id", roots.inventoryRootId)
        assertEquals(
            LoginInventoryFolder(
                folderId = "landmarks-folder-id",
                parentId = "inventory-root-id",
                ownerId = "agent-id",
                name = "Landmarks",
                typeDefault = 3,
                version = 42,
            ),
            roots.inventorySkeleton.single(),
        )
    }

    @Test
    fun `active session without protocol agent identity fails identity check`() {
        val session = hostessSession("live-session")
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            LibomvClientSession.active(session).requireIdentity(session),
        ).failure

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol agent identity unavailable", failure.redactedMessage)
    }

    @Test
    fun `active session without simulator identity fails identity check`() {
        val session = hostessSession("live-session")
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            LibomvClientSession.active(
                session = session,
                agentId = "agent-id",
                seedCapability = "seed-capability",
                simulatorIp = "203.0.113.8",
                simulatorPort = 13000,
                regionHandle = 123456789L,
            ).requireIdentity(session),
        ).failure

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol circuit identity unavailable", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("203.0.113.8"))
        assertFalse(failure.redactedMessage.orEmpty().contains("seed-capability"))
    }

    @Test
    fun `clear removes private identity fields`() {
        val oldSession = hostessSession("old-session")
        val newSession = hostessSession("new-session")
        val clientSession = activeSession(oldSession)

        clientSession.clear()
        clientSession.activate(newSession, agentId = "agent-id")

        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            clientSession.requireIdentity(newSession),
        ).failure
        assertEquals("protocol seed identity unavailable", failure.redactedMessage)
        assertEquals(
            LoginInventoryRoots.empty(),
            assertIs<LoginInventoryRootsResult.Success>(clientSession.inventoryRoots(newSession)).roots,
        )
        assertEquals(
            LoginAppearanceState.empty(),
            assertIs<LoginAppearanceStateResult.Success>(clientSession.appearanceState(newSession)).appearanceState,
        )
        val newIdentity = LibomvSessionIdentity(
            agentId = "agent-id",
            sessionId = "new-session",
            seedCapability = "seed-capability",
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
        )
        assertNull(clientSession.capabilityCache(newIdentity)?.urlFor(CapabilityName.EVENT_QUEUE_GET))
    }

    @Test
    fun `mismatched Hostess session fails without leaking IDs`() {
        val clientSession = LibomvClientSession.active(hostessSession("live-session"))
        val failure = clientSession.requireSession(hostessSession("other-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("hostess session mismatch", failure?.redactedMessage)
        assertFalse(failure?.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure?.redactedMessage.orEmpty().contains("other-session"))
        assertEquals(
            "hostess session mismatch",
            assertIs<LoginInventoryRootsResult.Failure>(
                clientSession.inventoryRoots(hostessSession("other-session")),
            ).failure.redactedMessage,
        )
        assertEquals(
            "hostess session mismatch",
            assertIs<LoginAppearanceStateResult.Failure>(
                clientSession.appearanceState(hostessSession("other-session")),
            ).failure.redactedMessage,
        )
    }

    @Test
    fun `inactive protocol session fails binding check`() {
        val failure = LibomvClientSession.inactive().requireSession(hostessSession("live-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("protocol session inactive", failure?.redactedMessage)
    }

    @Test
    fun `inactive Hostess session fails binding check`() {
        val session = hostessSession("live-session")
        val failure = LibomvClientSession.active(session).requireSession(session.copy(isActive = false))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("hostess session inactive", failure?.redactedMessage)
    }

    @Test
    fun `inactive protocol session does not expose capability cache`() {
        val session = hostessSession("live-session").copy(isActive = false)
        val clientSession = LibomvClientSession.active(session)

        assertNull(clientSession.capabilityCache(identityFor(session)))
    }

    private fun hostessSession(id: String): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private fun activeSession(session: HostessSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = "agent-id",
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
            agentName = "Venue Host",
            inventoryRoots = LoginInventoryRoots(
            inventoryRootId = "inventory-root-id",
            inventorySkeleton = listOf(
                LoginInventoryFolder(
                    folderId = "landmarks-folder-id",
                    parentId = "inventory-root-id",
                    ownerId = "agent-id",
                    name = "Landmarks",
                    typeDefault = 3,
                    version = 42,
                ),
            ),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        ),
        appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 51),
        capabilityCache = CapabilityCache(
            mapOf(CapabilityName.EVENT_QUEUE_GET to CapabilityUrl("https://caps.example/event")),
        ),
    )

    private fun identityFor(session: HostessSession): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = "agent-id",
        sessionId = session.sessionId.value,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
        agentName = "Venue Host",
    )
}
