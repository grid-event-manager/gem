package org.gem.core.services

data class GemCredentialRuntimeUnavailable(
    val reason: GemCredentialRuntimeUnavailableReason,
    val message: String? = null,
) : GemCredentialRuntimeState
