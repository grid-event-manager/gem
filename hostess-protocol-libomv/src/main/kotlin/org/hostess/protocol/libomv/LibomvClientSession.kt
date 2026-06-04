package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason

class LibomvClientSession private constructor(
    private val protocolState: ProtocolState,
) {
    fun unavailable(reason: CoreFailureReason): CoreFailure =
        CoreFailure(reason, redactedMessage = "protocol bootstrap unavailable")

    fun isProtocolAvailable(): Boolean = protocolState is ProtocolState.Available

    private sealed interface ProtocolState {
        data object Unavailable : ProtocolState
        data object Available : ProtocolState
    }

    companion object {
        fun unavailable(): LibomvClientSession = LibomvClientSession(ProtocolState.Unavailable)
    }
}
