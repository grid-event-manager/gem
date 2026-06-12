package org.gem.protocol.libomv

import org.gem.core.domain.GemInstant
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemSession
import org.gem.core.domain.SessionId
import org.gem.protocol.libomv.mapping.LoginAppearanceState
import org.gem.protocol.libomv.mapping.LoginAppearanceStateResult
import org.gem.protocol.libomv.mapping.LoginInventoryFolder
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import org.gem.protocol.libomv.mapping.LoginInventoryRootsResult
import org.gem.protocol.libomv.transport.CapabilityCache
import org.gem.protocol.libomv.transport.CapabilityName
import org.gem.protocol.libomv.transport.CapabilityUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class LibomvClientSessionTest {
    @Test
    fun `matching active Hostess session passes binding check`() {
        val session = gemSession("live-session")
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
        val session = gemSession("live-session")
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            LibomvClientSession.active(session).requireIdentity(session),
        ).failure

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol agent identity unavailable", failure.redactedMessage)
    }

    @Test
    fun `active session without simulator identity fails identity check`() {
        val session = gemSession("live-session")
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
        val oldSession = gemSession("old-session")
        val newSession = gemSession("new-session")
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
        val clientSession = LibomvClientSession.active(gemSession("live-session"))
        val failure = clientSession.requireSession(gemSession("other-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("gem session mismatch", failure?.redactedMessage)
        assertFalse(failure?.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure?.redactedMessage.orEmpty().contains("other-session"))
        assertEquals(
            "gem session mismatch",
            assertIs<LoginInventoryRootsResult.Failure>(
                clientSession.inventoryRoots(gemSession("other-session")),
            ).failure.redactedMessage,
        )
        assertEquals(
            "gem session mismatch",
            assertIs<LoginAppearanceStateResult.Failure>(
                clientSession.appearanceState(gemSession("other-session")),
            ).failure.redactedMessage,
        )
    }

    @Test
    fun `inactive protocol session fails binding check`() {
        val failure = LibomvClientSession.inactive().requireSession(gemSession("live-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("protocol session inactive", failure?.redactedMessage)
    }

    @Test
    fun `inactive Hostess session fails binding check`() {
        val session = gemSession("live-session")
        val failure = LibomvClientSession.active(session).requireSession(session.copy(isActive = false))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("gem session inactive", failure?.redactedMessage)
    }

    @Test
    fun `inactive protocol session does not expose capability cache`() {
        val session = gemSession("live-session").copy(isActive = false)
        val clientSession = LibomvClientSession.active(session)

        assertNull(clientSession.capabilityCache(identityFor(session)))
    }

    private fun gemSession(id: String): GemSession = GemSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun activeSession(session: GemSession): LibomvClientSession = LibomvClientSession.active(
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

    private fun identityFor(session: GemSession): LibomvSessionIdentity = LibomvSessionIdentity(
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
