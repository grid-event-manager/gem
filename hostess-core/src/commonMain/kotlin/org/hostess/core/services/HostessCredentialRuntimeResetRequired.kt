package org.hostess.core.services

data class HostessCredentialRuntimeResetRequired(
    val reason: HostessCredentialRuntimeResetReason,
    val message: String? = null,
) : HostessCredentialRuntimeState
