package org.gem.ui.state

import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GemCredentialRuntimeResetRequired
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.core.services.GemCredentialRuntimeUnavailable

data class CredentialRuntimeUiState(
    val status: CredentialRuntimeUiStatus,
    val message: String? = null,
) {
    val ready: Boolean
        get() = status == CredentialRuntimeUiStatus.READY

    companion object {
        fun from(runtimeState: GemCredentialRuntimeState): CredentialRuntimeUiState =
            when (runtimeState) {
                is GemCredentialRuntimeReady -> CredentialRuntimeUiState(CredentialRuntimeUiStatus.READY)
                is GemCredentialRuntimeUnavailable -> CredentialRuntimeUiState(
                    status = CredentialRuntimeUiStatus.UNAVAILABLE,
                    message = runtimeState.message,
                )
                is GemCredentialRuntimeResetRequired -> CredentialRuntimeUiState(
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
