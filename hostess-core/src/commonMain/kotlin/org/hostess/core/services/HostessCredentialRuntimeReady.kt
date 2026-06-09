package org.hostess.core.services

data class HostessCredentialRuntimeReady(
    val credentialService: CredentialService,
) : HostessCredentialRuntimeState
