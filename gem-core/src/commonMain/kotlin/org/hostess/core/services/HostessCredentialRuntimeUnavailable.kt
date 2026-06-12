package org.hostess.core.services

data class HostessCredentialRuntimeUnavailable(
    val reason: HostessCredentialRuntimeUnavailableReason,
    val message: String? = null,
) : HostessCredentialRuntimeState
