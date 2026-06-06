package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.mapping.LoginInventoryRootsResult

class LibomvClientSession private constructor(
    private val protocolAvailable: Boolean,
    private var activeSession: HostessSession?,
    private var agentId: String?,
    private var seedCapability: String?,
    private var simulatorIp: String?,
    private var simulatorPort: Int?,
    private var regionHandle: Long?,
    private var circuitCode: Long?,
    private var inventoryRoots: LoginInventoryRoots,
) {
    fun unavailable(reason: CoreFailureReason): CoreFailure =
        CoreFailure(reason, redactedMessage = if (protocolAvailable) {
            "protocol runtime unavailable"
        } else {
            "protocol bootstrap unavailable"
        })

    fun isProtocolAvailable(): Boolean = protocolAvailable

    internal fun activate(
        session: HostessSession,
        agentId: String? = null,
        seedCapability: String? = null,
        simulatorIp: String? = null,
        simulatorPort: Int? = null,
        regionHandle: Long? = null,
        circuitCode: Long? = null,
        inventoryRoots: LoginInventoryRoots = LoginInventoryRoots.empty(),
    ) {
        activeSession = session
        this.agentId = agentId
        this.seedCapability = seedCapability
        this.simulatorIp = simulatorIp
        this.simulatorPort = simulatorPort
        this.regionHandle = regionHandle
        this.circuitCode = circuitCode
        this.inventoryRoots = inventoryRoots
    }

    internal fun clear() {
        activeSession = null
        agentId = null
        seedCapability = null
        simulatorIp = null
        simulatorPort = null
        regionHandle = null
        circuitCode = null
        inventoryRoots = LoginInventoryRoots.empty()
    }

    internal fun requireSession(session: HostessSession): CoreFailure? {
        if (!protocolAvailable) {
            return CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol bootstrap unavailable")
        }
        val active = activeSession
            ?: return CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol session inactive")
        return when {
            !active.isActive -> CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol session inactive")
            !session.isActive -> CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "hostess session inactive")
            active.sessionId != session.sessionId ->
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "hostess session mismatch")
            else -> null
        }
    }

    internal fun requireIdentity(session: HostessSession): LibomvSessionIdentityResult {
        val bindingFailure = requireSession(session)
        if (bindingFailure != null) {
            return LibomvSessionIdentityResult.Failure(bindingFailure)
        }
        val activeAgentId = agentId?.takeIf(String::isNotBlank)
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol agent identity unavailable"),
            )
        val activeSeedCapability = seedCapability?.takeIf(String::isNotBlank)
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol seed identity unavailable"),
            )
        val activeSimulatorIp = simulatorIp?.takeIf(String::isNotBlank)
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol simulator identity unavailable"),
            )
        val activeSimulatorPort = simulatorPort?.takeIf { it > 0 }
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol simulator identity unavailable"),
            )
        val activeRegionHandle = regionHandle
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol region identity unavailable"),
            )
        val activeCircuitCode = circuitCode?.takeIf { it > 0L }
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol circuit identity unavailable"),
            )
        return LibomvSessionIdentityResult.Success(
            LibomvSessionIdentity(
                agentId = activeAgentId,
                sessionId = session.sessionId.value,
                seedCapability = activeSeedCapability,
                simulatorIp = activeSimulatorIp,
                simulatorPort = activeSimulatorPort,
                regionHandle = activeRegionHandle,
                circuitCode = activeCircuitCode,
            ),
        )
    }

    internal fun inventoryRoots(session: HostessSession): LoginInventoryRootsResult {
        val bindingFailure = requireSession(session)
        if (bindingFailure != null) {
            return LoginInventoryRootsResult.Failure(bindingFailure)
        }
        return LoginInventoryRootsResult.Success(inventoryRoots)
    }

    companion object {
        fun unavailable(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = false,
            activeSession = null,
            agentId = null,
            seedCapability = null,
            simulatorIp = null,
            simulatorPort = null,
            regionHandle = null,
            circuitCode = null,
            inventoryRoots = LoginInventoryRoots.empty(),
        )

        fun inactive(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = null,
            agentId = null,
            seedCapability = null,
            simulatorIp = null,
            simulatorPort = null,
            regionHandle = null,
            circuitCode = null,
            inventoryRoots = LoginInventoryRoots.empty(),
        )

        internal fun active(
            session: HostessSession,
            agentId: String? = null,
            seedCapability: String? = null,
            simulatorIp: String? = null,
            simulatorPort: Int? = null,
            regionHandle: Long? = null,
            circuitCode: Long? = null,
            inventoryRoots: LoginInventoryRoots = LoginInventoryRoots.empty(),
        ): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = session,
            agentId = agentId,
            seedCapability = seedCapability,
            simulatorIp = simulatorIp,
            simulatorPort = simulatorPort,
            regionHandle = regionHandle,
            circuitCode = circuitCode,
            inventoryRoots = inventoryRoots,
        )
    }
}

internal data class LibomvSessionIdentity(
    val agentId: String,
    val sessionId: String,
    val seedCapability: String,
    val simulatorIp: String,
    val simulatorPort: Int,
    val regionHandle: Long,
    val circuitCode: Long,
)

internal sealed interface LibomvSessionIdentityResult {
    data class Success(val identity: LibomvSessionIdentity) : LibomvSessionIdentityResult
    data class Failure(val failure: CoreFailure) : LibomvSessionIdentityResult
}
