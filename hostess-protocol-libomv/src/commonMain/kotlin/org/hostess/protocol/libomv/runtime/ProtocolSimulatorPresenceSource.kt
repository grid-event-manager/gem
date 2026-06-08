package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorPresenceResult
import org.hostess.protocol.libomv.transport.SimulatorPresenceStatus
import org.hostess.protocol.libomv.transport.toSimulatorCircuit

internal class ProtocolSimulatorPresenceSource(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : SimulatorPresenceSource {
    override fun simulatorPresence(identity: LibomvSessionIdentity): SimulatorPresenceProofResult =
        when (val result = circuitClient.ensurePresence(identity.toSimulatorCircuit())) {
            is SimulatorPresenceResult.Present -> SimulatorPresenceProofResult.Success(
                SimulatorPresenceProof(
                    simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
                    regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                    regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                    agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
                    agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
                    pingReplies = result.pingReplies,
                ),
            )
            is SimulatorPresenceResult.Failed -> result.toPresenceFailure()
        }

    private fun SimulatorPresenceResult.Failed.toPresenceFailure(): SimulatorPresenceProofResult.Failure {
        val proof = status.toProof()
        return SimulatorPresenceProofResult.Failure(
            proof = proof,
            failure = CoreFailure(
                reason = CoreFailureReason.GROUP_LIST_FAILED,
                redactedMessage = proof.redactedMessage,
            ),
        )
    }

    private fun SimulatorPresenceStatus.toProof(): SimulatorPresenceProof {
        val simulatorStatus = toProofStatus()
        return when (this) {
            SimulatorPresenceStatus.CIRCUIT_INVALID -> failureProof(
                simulatorStatus = SimulatorPresenceProofStatus.BLOCKED,
                regionHandshakeStatus = SimulatorPresenceProofStatus.NOT_RUN,
                message = "simulator presence blocked",
            )
            SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED,
            SimulatorPresenceStatus.SEND_FAILED,
            -> failureProof(
                simulatorStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                regionHandshakeStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                message = "simulator presence transport_gap",
            )
            SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
            SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
            -> failureProof(
                simulatorStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                message = "simulator presence proof_gap",
            )
            SimulatorPresenceStatus.PING_REPLY_FAILED -> failureProof(
                simulatorStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                regionHandshakeStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                message = "simulator presence transport_gap",
            )
            SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED -> failureProof(
                simulatorStatus = simulatorStatus,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                message = "simulator presence transport_gap",
            )
            SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED -> failureProof(
                simulatorStatus = simulatorStatus,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                message = "simulator presence transport_gap",
            )
            SimulatorPresenceStatus.MOVEMENT_TIMEOUT -> failureProof(
                simulatorStatus = simulatorStatus,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                message = "simulator presence proof_gap",
            )
            SimulatorPresenceStatus.AGENT_UPDATE_FAILED -> failureProof(
                simulatorStatus = simulatorStatus,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
                agentUpdateStatus = SimulatorPresenceProofStatus.TRANSPORT_GAP,
                message = "simulator presence transport_gap",
            )
        }
    }

    private fun SimulatorPresenceStatus.toProofStatus(): SimulatorPresenceProofStatus =
        when (this) {
            SimulatorPresenceStatus.CIRCUIT_INVALID -> SimulatorPresenceProofStatus.BLOCKED
            SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
            SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
            SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
            -> SimulatorPresenceProofStatus.PROOF_GAP
            SimulatorPresenceStatus.SEND_FAILED,
            SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED,
            SimulatorPresenceStatus.PING_REPLY_FAILED,
            SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED,
            SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED,
            SimulatorPresenceStatus.AGENT_UPDATE_FAILED,
            -> SimulatorPresenceProofStatus.TRANSPORT_GAP
        }

    private fun failureProof(
        simulatorStatus: SimulatorPresenceProofStatus,
        regionHandshakeStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.NOT_RUN,
        regionHandshakeReplyStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.NOT_RUN,
        agentMovementStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.NOT_RUN,
        agentUpdateStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.NOT_RUN,
        message: String,
    ): SimulatorPresenceProof = SimulatorPresenceProof(
        simulatorPresenceStatus = simulatorStatus,
        regionHandshakeStatus = regionHandshakeStatus,
        regionHandshakeReplyStatus = regionHandshakeReplyStatus,
        agentMovementStatus = agentMovementStatus,
        agentUpdateStatus = agentUpdateStatus,
        redactedMessage = message,
    )
}
