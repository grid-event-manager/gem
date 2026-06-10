package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessProofStatus
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.LibomvSessionIdentityResult
import org.hostess.protocol.libomv.mapping.LoginAppearanceStateResult
import org.hostess.protocol.libomv.mapping.LoginInventoryRootsResult
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.RegionProtocolFlags
import org.hostess.protocol.libomv.transport.SimulatorPresenceResult
import org.hostess.protocol.libomv.transport.SimulatorPresenceStatus
import org.hostess.protocol.libomv.transport.toSimulatorCircuit

class ProtocolAvatarRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val simulatorPresenceSource: AvatarSimulatorPresenceSource,
    private val capabilityUrlProvider: CapabilityUrlProvider,
    private val currentOutfitVersionSource: CurrentOutfitVersionSource = CurrentOutfitVersionSource(),
    private val appearanceSource: AvatarAppearanceSource = AvatarAppearanceSource.unavailable(),
) {
    internal constructor(
        clientSession: LibomvClientSession,
        circuitClient: ProtocolSimulatorCircuitClient,
        capabilityUrlProvider: CapabilityUrlProvider,
        currentOutfitVersionSource: CurrentOutfitVersionSource = CurrentOutfitVersionSource(),
        appearanceSource: AvatarAppearanceSource = AvatarAppearanceSource.unavailable(),
    ) : this(
        clientSession = clientSession,
        simulatorPresenceSource = AvatarSimulatorPresenceSource { identity ->
            circuitClient.ensurePresence(identity.toSimulatorCircuit())
        },
        capabilityUrlProvider = capabilityUrlProvider,
        currentOutfitVersionSource = currentOutfitVersionSource,
        appearanceSource = appearanceSource,
    )

    fun ensureReady(session: HostessSession): AvatarReadinessResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure ->
                return failure(
                    status = AvatarReadinessProofStatus.RUNTIME_GAP,
                    simulatorPresenceStatus = AvatarReadinessProofStatus.NOT_RUN,
                    message = result.failure.redactedMessage ?: "avatar readiness session unavailable",
                )
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        val roots = when (val result = clientSession.inventoryRoots(session)) {
            is LoginInventoryRootsResult.Failure ->
                return failure(
                    status = AvatarReadinessProofStatus.RUNTIME_GAP,
                    simulatorPresenceStatus = AvatarReadinessProofStatus.NOT_RUN,
                    message = result.failure.redactedMessage ?: "avatar readiness inventory roots unavailable",
                )
            is LoginInventoryRootsResult.Success -> result.roots
        }
        val appearanceState = when (val result = clientSession.appearanceState(session)) {
            is LoginAppearanceStateResult.Failure ->
                return failure(
                    status = AvatarReadinessProofStatus.RUNTIME_GAP,
                    simulatorPresenceStatus = AvatarReadinessProofStatus.NOT_RUN,
                    message = result.failure.redactedMessage ?: "avatar readiness appearance state unavailable",
                )
            is LoginAppearanceStateResult.Success -> result.appearanceState
        }
        val presence = when (val result = simulatorPresenceSource.ensurePresence(identity)) {
            is SimulatorPresenceResult.Failed -> return presenceFailure(result.status, result.redactedMessage)
            is SimulatorPresenceResult.Present -> result
        }
        if (!presence.regionProtocolFlags.agentAppearanceService) {
            return failure(
                status = AvatarReadinessProofStatus.BLOCKED,
                simulatorPresenceStatus = AvatarReadinessProofStatus.PASSED,
                regionProtocolStatus = AvatarReadinessProofStatus.PASSED,
                agentAppearanceServiceStatus = AvatarReadinessProofStatus.BLOCKED,
                message = "agent appearance service unavailable",
                regionName = presence.regionName,
            )
        }
        val cofVersion = when (
            val result = currentOutfitVersionSource.currentVersion(identity, roots, appearanceState)
        ) {
            is CurrentOutfitVersionResult.Available -> result.version
            is CurrentOutfitVersionResult.Unavailable ->
                return failure(
                    status = AvatarReadinessProofStatus.BLOCKED,
                    simulatorPresenceStatus = AvatarReadinessProofStatus.PASSED,
                    regionProtocolStatus = AvatarReadinessProofStatus.PASSED,
                    agentAppearanceServiceStatus = AvatarReadinessProofStatus.PASSED,
                    cofVersionStatus = AvatarReadinessProofStatus.BLOCKED,
                    message = result.redactedMessage,
                    regionName = presence.regionName,
                )
        }
        val capabilityUrl = when (
            val result = capabilityUrlProvider.requireUrl(identity, CapabilityName.UPDATE_AVATAR_APPEARANCE)
        ) {
            is CapabilityUrlResult.Ready -> result.url
            is CapabilityUrlResult.TransportGap ->
                return appearanceFailure(
                    status = AvatarReadinessProofStatus.TRANSPORT_GAP,
                    message = "avatar appearance capability unavailable",
                    regionName = presence.regionName,
                )
            is CapabilityUrlResult.MappingGap ->
                return appearanceFailure(
                    status = AvatarReadinessProofStatus.PROOF_GAP,
                    message = "avatar appearance capability unavailable",
                    regionName = presence.regionName,
                )
        }
        return when (val result = appearanceSource.updateServerAppearance(identity, cofVersion, capabilityUrl)) {
            AvatarAppearanceUpdateResult.Success -> AvatarReadinessResult.Success(
                AvatarReadinessProof.success(regionName = presence.regionName),
            )
            is AvatarAppearanceUpdateResult.TransportGap ->
                appearanceFailure(
                    status = AvatarReadinessProofStatus.TRANSPORT_GAP,
                    message = result.redactedMessage,
                    regionName = presence.regionName,
                )
            is AvatarAppearanceUpdateResult.ProofGap ->
                appearanceFailure(
                    status = AvatarReadinessProofStatus.PROOF_GAP,
                    message = result.redactedMessage,
                    regionName = presence.regionName,
                )
        }
    }

    private fun presenceFailure(
        status: SimulatorPresenceStatus,
        redactedMessage: String,
    ): AvatarReadinessResult.Failure {
        val proofStatus = status.toReadinessStatus()
        return failure(
            status = proofStatus,
            simulatorPresenceStatus = proofStatus,
            message = "simulator presence ${proofStatus.reportValue}: $redactedMessage",
        )
    }

    private fun appearanceFailure(
        status: AvatarReadinessProofStatus,
        message: String,
        regionName: String?,
    ): AvatarReadinessResult.Failure =
        failure(
            status = status,
            simulatorPresenceStatus = AvatarReadinessProofStatus.PASSED,
            regionProtocolStatus = AvatarReadinessProofStatus.PASSED,
            agentAppearanceServiceStatus = AvatarReadinessProofStatus.PASSED,
            cofVersionStatus = AvatarReadinessProofStatus.PASSED,
            serverAppearanceStatus = status,
            message = message,
            regionName = regionName,
        )

    private fun failure(
        status: AvatarReadinessProofStatus,
        simulatorPresenceStatus: AvatarReadinessProofStatus,
        regionProtocolStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.NOT_RUN,
        agentAppearanceServiceStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.NOT_RUN,
        cofVersionStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.NOT_RUN,
        serverAppearanceStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.NOT_RUN,
        regionName: String? = null,
        message: String,
    ): AvatarReadinessResult.Failure = AvatarReadinessResult.Failure(
        proof = AvatarReadinessProof(
            avatarReadinessStatus = status,
            simulatorPresenceStatus = simulatorPresenceStatus,
            regionProtocolStatus = regionProtocolStatus,
            agentAppearanceServiceStatus = agentAppearanceServiceStatus,
            cofVersionStatus = cofVersionStatus,
            serverAppearanceStatus = serverAppearanceStatus,
            regionName = regionName,
        ),
        failure = CoreFailure(
            reason = CoreFailureReason.AVATAR_READINESS_FAILED,
            redactedMessage = message,
        ),
    )

    private fun SimulatorPresenceStatus.toReadinessStatus(): AvatarReadinessProofStatus =
        when (this) {
            SimulatorPresenceStatus.CIRCUIT_INVALID -> AvatarReadinessProofStatus.RUNTIME_GAP
            SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
            SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
            SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
            -> AvatarReadinessProofStatus.PROOF_GAP
            SimulatorPresenceStatus.SEND_FAILED,
            SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED,
            SimulatorPresenceStatus.PING_REPLY_FAILED,
            SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED,
            SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED,
            SimulatorPresenceStatus.AGENT_UPDATE_FAILED,
            -> AvatarReadinessProofStatus.TRANSPORT_GAP
        }
}

internal fun interface AvatarSimulatorPresenceSource {
    fun ensurePresence(identity: LibomvSessionIdentity): SimulatorPresenceResult
}
