package org.gem.core.services

data class GemCredentialRuntimeResetRequired(
    val reason: GemCredentialRuntimeResetReason,
    val message: String? = null,
) : GemCredentialRuntimeState
