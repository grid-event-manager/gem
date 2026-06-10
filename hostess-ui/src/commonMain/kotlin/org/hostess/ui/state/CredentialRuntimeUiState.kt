package org.hostess.ui.state

import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeResetRequired
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.core.services.HostessCredentialRuntimeUnavailable

data class CredentialRuntimeUiState(
    val status: CredentialRuntimeUiStatus,
    val message: String? = null,
) {
    val ready: Boolean
        get() = status == CredentialRuntimeUiStatus.READY

    companion object {
        fun from(runtimeState: HostessCredentialRuntimeState): CredentialRuntimeUiState =
            when (runtimeState) {
                is HostessCredentialRuntimeReady -> CredentialRuntimeUiState(CredentialRuntimeUiStatus.READY)
                is HostessCredentialRuntimeUnavailable -> CredentialRuntimeUiState(
                    status = CredentialRuntimeUiStatus.UNAVAILABLE,
                    message = runtimeState.message,
                )
                is HostessCredentialRuntimeResetRequired -> CredentialRuntimeUiState(
                    status = CredentialRuntimeUiStatus.RESET_REQUIRED,
                    message = runtimeState.message,
                )
            }
    }
}

enum class CredentialRuntimeUiStatus {
    READY,
    UNAVAILABLE,
    RESET_REQUIRED,
}
