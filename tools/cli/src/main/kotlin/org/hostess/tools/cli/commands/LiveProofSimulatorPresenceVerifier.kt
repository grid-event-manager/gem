package org.hostess.tools.cli.commands

import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.core.services.GroupDirectoryService

internal class LiveProofSimulatorPresenceVerifier(
    private val groupDirectoryService: GroupDirectoryService,
) {
    fun verify(session: HostessSession): LiveProofSimulatorPresenceOutcome =
        when (val result = groupDirectoryService.simulatorPresence(session)) {
            is SimulatorPresenceProofResult.Success -> {
                val passed = result.proof.allPassed()
                LiveProofSimulatorPresenceOutcome(
                    passed = passed,
                    statusFields = statusFields(result.proof),
                    step = if (passed) {
                        LiveProofStep.passed("simulator-presence", detail(result.proof))
                    } else {
                        LiveProofStep(
                            "simulator-presence",
                            result.proof.stepStatus(),
                            result.proof.redactedMessage ?: "simulator presence proof_gap",
                        )
                    },
                    failureReason = if (passed) {
                        null
                    } else {
                        result.proof.redactedMessage ?: "simulator presence proof_gap"
                    },
                )
            }
            is SimulatorPresenceProofResult.Failure -> {
                val detail = result.proof.redactedMessage
                    ?: result.failure.redactedMessage
                    ?: result.failure.reason.name.lowercase()
                LiveProofSimulatorPresenceOutcome(
                    passed = false,
                    statusFields = statusFields(result.proof),
                    step = LiveProofStep("simulator-presence", result.proof.stepStatus(), detail),
                    failureReason = detail,
                )
            }
        }

    private fun statusFields(proof: SimulatorPresenceProof): Map<String, String> = mapOf(
        "simulatorPresenceStatus" to proof.simulatorPresenceStatus.reportValue,
        "simulatorSessionStatus" to proof.simulatorPresenceStatus.reportValue,
        "simulatorHeartbeatStatus" to proof.heartbeatStatus.reportValue,
        "regionHandshakeStatus" to proof.regionHandshakeStatus.reportValue,
        "regionHandshakeReplyStatus" to proof.regionHandshakeReplyStatus.reportValue,
        "agentMovementStatus" to proof.agentMovementStatus.reportValue,
        "agentUpdateStatus" to proof.agentUpdateStatus.reportValue,
    )

    private fun detail(proof: SimulatorPresenceProof): String =
        "pingReplies=${proof.pingReplies}; heartbeat=${proof.heartbeatStatus.reportValue}"

    private fun SimulatorPresenceProof.allPassed(): Boolean =
        listOf(
            simulatorPresenceStatus,
            heartbeatStatus,
            regionHandshakeStatus,
            regionHandshakeReplyStatus,
            agentMovementStatus,
            agentUpdateStatus,
        ).all { it == SimulatorPresenceProofStatus.PASSED }

    private fun SimulatorPresenceProof.stepStatus(): String =
        listOf(
            simulatorPresenceStatus,
            heartbeatStatus,
            regionHandshakeStatus,
            regionHandshakeReplyStatus,
            agentMovementStatus,
            agentUpdateStatus,
        ).firstOrNull { it != SimulatorPresenceProofStatus.PASSED }?.reportValue
            ?: SimulatorPresenceProofStatus.PASSED.reportValue
}

internal data class LiveProofSimulatorPresenceOutcome(
    val passed: Boolean,
    val statusFields: Map<String, String>,
    val step: LiveProofStep,
    val failureReason: String?,
)
