package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AvatarReadinessProofStatus
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LoginAppearanceState
import org.hostess.protocol.libomv.mapping.LoginInventoryFolder
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import org.hostess.protocol.libomv.transport.RegionProtocolFlags
import org.hostess.protocol.libomv.transport.SimulatorPresenceResult
import org.hostess.protocol.libomv.transport.SimulatorPresenceStatus

class ProtocolAvatarRuntimeTest {
    @Test
    fun `presence true plus cof capability and server update passes all statuses in order`() {
        val session = hostessSession()
        val calls = mutableListOf<String>()
        val presence = RecordingPresenceSource(calls)
        val cof = RecordingCofSource(calls, CurrentOutfitVersionResult.Available(17))
        val capability = RecordingCapabilityProvider(calls, CapabilityUrlResult.Ready(capabilityUrl()))
        val appearance = RecordingAppearanceSource(calls, AvatarAppearanceUpdateResult.Success)

        val result = assertIs<AvatarReadinessResult.Success>(
            runtime(session, presence, cof, capability, appearance).ensureReady(session),
        )

        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.simulatorPresenceStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.regionProtocolStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.agentAppearanceServiceStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.cofVersionStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.serverAppearanceStatus)
        assertEquals("London City", result.proof.regionName)
        assertEquals(listOf("presence", "cof", "cap:UpdateAvatarAppearance", "appearance"), calls)
        assertEquals(listOf(CapabilityName.UPDATE_AVATAR_APPEARANCE), capability.names)
        assertEquals(listOf(17), appearance.cofVersions)
    }

    @Test
    fun `missing simulator region name keeps readiness success with blank region proof`() {
        val session = hostessSession()
        val calls = mutableListOf<String>()
        val presence = RecordingPresenceSource(
            calls,
            SimulatorPresenceResult.Present(
                pingReplies = 0,
                cached = false,
                regionName = null,
                regionProtocolFlags = RegionProtocolFlags(agentAppearanceService = true),
            ),
        )
        val cof = RecordingCofSource(calls, CurrentOutfitVersionResult.Available(17))
        val capability = RecordingCapabilityProvider(calls, CapabilityUrlResult.Ready(capabilityUrl()))
        val appearance = RecordingAppearanceSource(calls, AvatarAppearanceUpdateResult.Success)

        val result = assertIs<AvatarReadinessResult.Success>(
            runtime(session, presence, cof, capability, appearance).ensureReady(session),
        )

        assertNull(result.proof.regionName)
        assertEquals(listOf("presence", "cof", "cap:UpdateAvatarAppearance", "appearance"), calls)
    }

    @Test
    fun `current simulator region flags block readiness and login appearance state does not override`() {
        val session = hostessSession()
        val calls = mutableListOf<String>()
        val presence = RecordingPresenceSource(
            calls,
            SimulatorPresenceResult.Present(
                pingReplies = 0,
                cached = false,
                regionName = "London City",
                regionProtocolFlags = RegionProtocolFlags(agentAppearanceService = false),
            ),
        )
        val cof = RecordingCofSource(calls)
        val capability = RecordingCapabilityProvider(calls)
        val appearance = RecordingAppearanceSource(calls)

        val failure = assertIs<AvatarReadinessResult.Failure>(
            runtime(
                session = session,
                presence = presence,
                cof = cof,
                capability = capability,
                appearance = appearance,
                appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 17),
            ).ensureReady(session),
        )

        assertEquals("agent appearance service unavailable", failure.failure.redactedMessage)
        assertEquals(AvatarReadinessProofStatus.BLOCKED, failure.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, failure.proof.simulatorPresenceStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, failure.proof.regionProtocolStatus)
        assertEquals(AvatarReadinessProofStatus.BLOCKED, failure.proof.agentAppearanceServiceStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.cofVersionStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.serverAppearanceStatus)
        assertEquals("London City", failure.proof.regionName)
        assertEquals(listOf("presence"), calls)
    }

    @Test
    fun `missing cof blocks before capability or http calls`() {
        val session = hostessSession()
        val calls = mutableListOf<String>()
        val failure = assertIs<AvatarReadinessResult.Failure>(
            runtime(
                session = session,
                presence = RecordingPresenceSource(calls),
                cof = RecordingCofSource(calls, CurrentOutfitVersionResult.Unavailable("cof version unavailable")),
                capability = RecordingCapabilityProvider(calls),
                appearance = RecordingAppearanceSource(calls),
            ).ensureReady(session),
        )

        assertEquals("cof version unavailable", failure.failure.redactedMessage)
        assertEquals(AvatarReadinessProofStatus.BLOCKED, failure.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.PASSED, failure.proof.agentAppearanceServiceStatus)
        assertEquals(AvatarReadinessProofStatus.BLOCKED, failure.proof.cofVersionStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.serverAppearanceStatus)
        assertEquals(listOf("presence", "cof"), calls)
    }

    @Test
    fun `capability gaps classify server appearance stop point`() {
        val session = hostessSession()
        val cases = listOf(
            CapabilityUrlResult.TransportGap("raw caps timeout") to AvatarReadinessProofStatus.TRANSPORT_GAP,
            CapabilityUrlResult.MappingGap("raw caps missing") to AvatarReadinessProofStatus.PROOF_GAP,
        )

        cases.forEach { (capabilityResult, expectedStatus) ->
            val calls = mutableListOf<String>()
            val failure = assertIs<AvatarReadinessResult.Failure>(
                runtime(
                    session = session,
                    presence = RecordingPresenceSource(calls),
                    cof = RecordingCofSource(calls, CurrentOutfitVersionResult.Available(17)),
                    capability = RecordingCapabilityProvider(calls, capabilityResult),
                    appearance = RecordingAppearanceSource(calls),
                ).ensureReady(session),
            )

            assertEquals("avatar appearance capability unavailable", failure.failure.redactedMessage)
            assertEquals(expectedStatus, failure.proof.avatarReadinessStatus)
            assertEquals(expectedStatus, failure.proof.serverAppearanceStatus)
            assertEquals(listOf("presence", "cof", "cap:UpdateAvatarAppearance"), calls)
        }
    }

    @Test
    fun `appearance source failures classify and redact without ids urls or cof value`() {
        val session = hostessSession()
        val cases = listOf(
            AvatarAppearanceUpdateResult.TransportGap("avatar appearance transport unavailable") to
                AvatarReadinessProofStatus.TRANSPORT_GAP,
            AvatarAppearanceUpdateResult.ProofGap("avatar appearance response invalid") to
                AvatarReadinessProofStatus.PROOF_GAP,
        )

        cases.forEach { (sourceResult, expectedStatus) ->
            val calls = mutableListOf<String>()
            val failure = assertIs<AvatarReadinessResult.Failure>(
                runtime(
                    session = session,
                    presence = RecordingPresenceSource(calls),
                    cof = RecordingCofSource(calls, CurrentOutfitVersionResult.Available(17)),
                    capability = RecordingCapabilityProvider(calls, CapabilityUrlResult.Ready(capabilityUrl())),
                    appearance = RecordingAppearanceSource(calls, sourceResult),
                ).ensureReady(session),
            )

            val message = failure.failure.redactedMessage.orEmpty()
            assertEquals(expectedStatus, failure.proof.avatarReadinessStatus)
            assertEquals(expectedStatus, failure.proof.serverAppearanceStatus)
            assertFalse(message.contains(AGENT_ID))
            assertFalse(message.contains(SESSION_ID))
            assertFalse(message.contains(capabilityUrl().value))
            assertFalse(message.contains("17"))
            assertEquals(listOf("presence", "cof", "cap:UpdateAvatarAppearance", "appearance"), calls)
        }
    }

    @Test
    fun `presence failures map to avatar readiness statuses`() {
        val session = hostessSession()
        val cases = listOf(
            SimulatorPresenceStatus.CIRCUIT_INVALID to AvatarReadinessProofStatus.RUNTIME_GAP,
            SimulatorPresenceStatus.SEND_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.PING_REPLY_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.AGENT_UPDATE_FAILED to AvatarReadinessProofStatus.TRANSPORT_GAP,
            SimulatorPresenceStatus.HANDSHAKE_TIMEOUT to AvatarReadinessProofStatus.PROOF_GAP,
            SimulatorPresenceStatus.HANDSHAKE_MALFORMED to AvatarReadinessProofStatus.PROOF_GAP,
            SimulatorPresenceStatus.MOVEMENT_TIMEOUT to AvatarReadinessProofStatus.PROOF_GAP,
        )

        cases.forEach { (presenceStatus, expectedStatus) ->
            val calls = mutableListOf<String>()
            val failure = assertIs<AvatarReadinessResult.Failure>(
                runtime(
                    session = session,
                    presence = RecordingPresenceSource(
                        calls,
                        SimulatorPresenceResult.Failed(presenceStatus, "raw simulator failure"),
                    ),
                    cof = RecordingCofSource(calls),
                    capability = RecordingCapabilityProvider(calls),
                    appearance = RecordingAppearanceSource(calls),
                ).ensureReady(session),
            )

            assertEquals(expectedStatus, failure.proof.avatarReadinessStatus)
            assertEquals(expectedStatus, failure.proof.simulatorPresenceStatus)
            assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.regionProtocolStatus)
            assertEquals(listOf("presence"), calls)
        }
    }

    @Test
    fun `mismatched session returns runtime gap before protocol calls`() {
        val calls = mutableListOf<String>()
        val runtime = runtime(
            session = hostessSession("live-session"),
            presence = RecordingPresenceSource(calls),
            cof = RecordingCofSource(calls),
            capability = RecordingCapabilityProvider(calls),
            appearance = RecordingAppearanceSource(calls),
        )

        val failure = assertIs<AvatarReadinessResult.Failure>(
            runtime.ensureReady(hostessSession("other-session")),
        )

        assertEquals(CoreFailureReason.AVATAR_READINESS_FAILED, failure.failure.reason)
        assertEquals("hostess session mismatch", failure.failure.redactedMessage)
        assertEquals(AvatarReadinessProofStatus.RUNTIME_GAP, failure.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, failure.proof.simulatorPresenceStatus)
        assertEquals(emptyList(), calls)
    }

    private fun runtime(
        session: HostessSession,
        presence: RecordingPresenceSource,
        cof: RecordingCofSource,
        capability: RecordingCapabilityProvider,
        appearance: RecordingAppearanceSource,
        appearanceState: LoginAppearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 17),
    ): ProtocolAvatarRuntime = ProtocolAvatarRuntime(
        clientSession = activeClientSession(session, appearanceState),
        simulatorPresenceSource = presence,
        capabilityUrlProvider = capability,
        currentOutfitVersionSource = cof,
        appearanceSource = appearance,
    )

    private fun activeClientSession(
        session: HostessSession,
        appearanceState: LoginAppearanceState,
    ): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
        inventoryRoots = LoginInventoryRoots(
            inventoryRootId = ROOT_FOLDER_ID,
            inventorySkeleton = listOf(
                LoginInventoryFolder(
                    folderId = CURRENT_OUTFIT_ID,
                    parentId = ROOT_FOLDER_ID,
                    ownerId = AGENT_ID,
                    name = "Current Outfit",
                    typeDefault = CurrentOutfitVersionSource.CURRENT_OUTFIT_FOLDER_TYPE,
                    version = 17,
                ),
            ),
            libraryRootId = null,
            libraryOwnerId = null,
            librarySkeleton = emptyList(),
        ),
        appearanceState = appearanceState,
    )

    private class RecordingPresenceSource(
        private val calls: MutableList<String>,
        var result: SimulatorPresenceResult = SimulatorPresenceResult.Present(
            pingReplies = 0,
            cached = false,
            regionName = "London City",
            regionProtocolFlags = RegionProtocolFlags(agentAppearanceService = true),
        ),
    ) : AvatarSimulatorPresenceSource {
        override fun ensurePresence(identity: LibomvSessionIdentity): SimulatorPresenceResult {
            calls += "presence"
            return result
        }
    }

    private class RecordingCofSource(
        private val calls: MutableList<String>,
        var result: CurrentOutfitVersionResult = CurrentOutfitVersionResult.Available(17),
    ) : CurrentOutfitVersionSource() {
        override fun currentVersion(
            identity: LibomvSessionIdentity,
            roots: LoginInventoryRoots,
            appearanceState: LoginAppearanceState,
        ): CurrentOutfitVersionResult {
            calls += "cof"
            return result
        }
    }

    private class RecordingCapabilityProvider(
        private val calls: MutableList<String>,
        var result: CapabilityUrlResult = CapabilityUrlResult.Ready(capabilityUrl()),
    ) : CapabilityUrlProvider {
        val names = mutableListOf<CapabilityName>()

        override fun requireUrl(
            identity: LibomvSessionIdentity,
            name: CapabilityName,
        ): CapabilityUrlResult {
            calls += "cap:${name.wireName}"
            names += name
            return result
        }
    }

    private class RecordingAppearanceSource(
        private val calls: MutableList<String>,
        var result: AvatarAppearanceUpdateResult = AvatarAppearanceUpdateResult.Success,
    ) : AvatarAppearanceSource {
        val cofVersions = mutableListOf<Int>()

        override fun updateServerAppearance(
            identity: LibomvSessionIdentity,
            cofVersion: Int,
            capabilityUrl: CapabilityUrl,
        ): AvatarAppearanceUpdateResult {
            calls += "appearance"
            cofVersions += cofVersion
            return result
        }
    }

    private fun hostessSession(id: String = SESSION_ID): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val ROOT_FOLDER_ID = "33333333-3333-3333-3333-333333333333"
        const val CURRENT_OUTFIT_ID = "44444444-4444-4444-4444-444444444444"

        fun capabilityUrl(): CapabilityUrl = CapabilityUrl("https://caps.example/appearance")
    }
}
