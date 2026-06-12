package org.gem.protocol.libomv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.SessionId
import org.gem.core.ports.AvatarReadinessProofStatus
import org.gem.core.ports.AvatarReadinessResult
import org.gem.protocol.libomv.mapping.LoginAppearanceState
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import org.gem.protocol.libomv.runtime.AvatarAppearanceUpdateResult
import org.gem.protocol.libomv.runtime.AvatarSimulatorPresenceSource
import org.gem.protocol.libomv.runtime.CurrentOutfitVersionResult
import org.gem.protocol.libomv.runtime.CurrentOutfitVersionSource
import org.gem.protocol.libomv.runtime.ProtocolAvatarRuntime
import org.gem.protocol.libomv.transport.CapabilityName
import org.gem.protocol.libomv.transport.CapabilityUrl
import org.gem.protocol.libomv.transport.CapabilityUrlProvider
import org.gem.protocol.libomv.transport.CapabilityUrlResult
import org.gem.protocol.libomv.transport.RegionProtocolFlags
import org.gem.protocol.libomv.transport.SimulatorPresenceResult

class LibomvAvatarAdapterTest {
    @Test
    fun `ensure ready routes through protocol runtime`() {
        val calls = mutableListOf<String>()
        val session = gemSession()
        val runtime = ProtocolAvatarRuntime(
            clientSession = activeClientSession(session),
            simulatorPresenceSource = AvatarSimulatorPresenceSource {
                calls += "presence"
                SimulatorPresenceResult.Present(
                    pingReplies = 0,
                    cached = false,
                    regionProtocolFlags = RegionProtocolFlags(agentAppearanceService = true),
                )
            },
            capabilityUrlProvider = CapabilityUrlProvider { _, name ->
                calls += "cap:${name.wireName}"
                CapabilityUrlResult.Ready(CapabilityUrl("https://caps.example/appearance"))
            },
            currentOutfitVersionSource = object : CurrentOutfitVersionSource() {
                override fun currentVersion(
                    identity: LibomvSessionIdentity,
                    roots: LoginInventoryRoots,
                    appearanceState: LoginAppearanceState,
                ): CurrentOutfitVersionResult {
                    calls += "cof"
                    return CurrentOutfitVersionResult.Available(17)
                }
            },
            appearanceSource = { _, _, _ ->
                calls += "appearance"
                AvatarAppearanceUpdateResult.Success
            },
        )

        val result = LibomvAvatarAdapter(runtime).ensureReady(session)

        assertIs<AvatarReadinessResult.Success>(result)
        assertEquals(listOf("presence", "cof", "cap:UpdateAvatarAppearance", "appearance"), calls)
    }

    @Test
    fun `ensure ready fallback fails closed without runtime`() {
        val result = LibomvAvatarAdapter().ensureReady(gemSession())

        val failure = assertIs<AvatarReadinessResult.Failure>(result)
        assertEquals(CoreFailureReason.AVATAR_READINESS_FAILED, failure.failure.reason)
        assertEquals("avatar readiness runtime unavailable", failure.failure.redactedMessage)
        assertEquals(AvatarReadinessProofStatus.RUNTIME_GAP, failure.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.simulatorPresenceStatus)
    }

    private fun activeClientSession(session: GemSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = "11111111-1111-1111-1111-111111111111",
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
        inventoryRoots = LoginInventoryRoots(
            inventoryRootId = "22222222-2222-2222-2222-222222222222",
            inventorySkeleton = emptyList(),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        ),
        appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 17),
    )

    private fun gemSession(): GemSession = GemSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )
}
